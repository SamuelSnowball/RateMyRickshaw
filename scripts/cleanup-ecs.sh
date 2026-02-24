#!/bin/bash

# Script to delete ECS service and associated scheduled rule

REGION="eu-west-2"
APP_NAME="ratemyrickshaw"

echo "========================================="
echo "ECS Service and Scheduled Rule Cleanup"
echo "========================================="
echo "Region: $REGION"
echo ""

# List ECS clusters to find the service
echo "Finding ECS clusters..."
CLUSTERS=$(aws ecs list-clusters --region $REGION --query 'clusterArns[*]' --output text)

if [ -z "$CLUSTERS" ]; then
    echo "No ECS clusters found in region $REGION"
else
    echo "Found clusters:"
    echo "$CLUSTERS"
    echo ""
    
    # For each cluster, list services
    for CLUSTER_ARN in $CLUSTERS; do
        CLUSTER_NAME=$(echo $CLUSTER_ARN | awk -F'/' '{print $NF}')
        echo "Checking cluster: $CLUSTER_NAME"
        
        SERVICES=$(aws ecs list-services --cluster $CLUSTER_NAME --region $REGION --query 'serviceArns[*]' --output text)
        
        if [ ! -z "$SERVICES" ]; then
            echo "Services in cluster $CLUSTER_NAME:"
            for SERVICE_ARN in $SERVICES; do
                SERVICE_NAME=$(echo $SERVICE_ARN | awk -F'/' '{print $NF}')
                echo "  - $SERVICE_NAME"
                
                # Ask for confirmation before deleting
                read -p "Delete service $SERVICE_NAME? (y/n): " -n 1 -r
                echo
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    echo "Updating service to 0 desired count..."
                    aws ecs update-service \
                        --cluster $CLUSTER_NAME \
                        --service $SERVICE_NAME \
                        --desired-count 0 \
                        --region $REGION
                    
                    echo "Waiting for service to drain..."
                    sleep 10
                    
                    echo "Deleting service..."
                    aws ecs delete-service \
                        --cluster $CLUSTER_NAME \
                        --service $SERVICE_NAME \
                        --force \
                        --region $REGION
                    
                    echo "✅ Service $SERVICE_NAME deleted"
                fi
            done
        fi
    done
fi

echo ""
echo "========================================="
echo "Finding EventBridge Scheduled Rules"
echo "========================================="

# List all EventBridge rules
RULES=$(aws events list-rules --region $REGION --query 'Rules[*].Name' --output text)

if [ -z "$RULES" ]; then
    echo "No EventBridge rules found"
else
    echo "Found rules:"
    for RULE in $RULES; do
        # Get rule details
        RULE_DETAILS=$(aws events describe-rule --name $RULE --region $REGION)
        SCHEDULE=$(echo "$RULE_DETAILS" | jq -r '.ScheduleExpression // "N/A"')
        STATE=$(echo "$RULE_DETAILS" | jq -r '.State')
        
        echo ""
        echo "Rule: $RULE"
        echo "  Schedule: $SCHEDULE"
        echo "  State: $STATE"
        
        # Check if it's a 10am scheduled rule or related to the app
        if echo "$SCHEDULE" | grep -q "cron(0 10" || echo "$RULE" | grep -qi "$APP_NAME"; then
            read -p "Delete this rule? (y/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                # Remove all targets first
                echo "Removing targets..."
                TARGETS=$(aws events list-targets-by-rule --rule $RULE --region $REGION --query 'Targets[*].Id' --output text)
                
                if [ ! -z "$TARGETS" ]; then
                    TARGET_IDS=$(echo $TARGETS | tr ' ' '\n' | jq -R . | jq -s .)
                    aws events remove-targets \
                        --rule $RULE \
                        --ids $(echo $TARGETS | tr ' ' '\n') \
                        --region $REGION
                    echo "Targets removed"
                fi
                
                # Delete the rule
                aws events delete-rule \
                    --name $RULE \
                    --region $REGION
                
                echo "✅ Rule $RULE deleted"
            fi
        fi
    done
fi

echo ""
echo "========================================="
echo "Cleanup Complete"
echo "========================================="
