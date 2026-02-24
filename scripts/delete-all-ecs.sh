#!/bin/bash

# Quick script to delete ALL ECS services and EventBridge rules (no prompts)

REGION="${1:-eu-west-2}"

echo "========================================="
echo "Deleting ALL ECS Services and Rules"
echo "========================================="
echo "Region: $REGION"
echo ""

# Delete all ECS services in all clusters
CLUSTERS=$(aws ecs list-clusters --region $REGION --query 'clusterArns[*]' --output text)

for CLUSTER_ARN in $CLUSTERS; do
    CLUSTER_NAME=$(echo $CLUSTER_ARN | awk -F'/' '{print $NF}')
    echo "Processing cluster: $CLUSTER_NAME"
    
    SERVICES=$(aws ecs list-services --cluster $CLUSTER_NAME --region $REGION --query 'serviceArns[*]' --output text)
    
    for SERVICE_ARN in $SERVICES; do
        SERVICE_NAME=$(echo $SERVICE_ARN | awk -F'/' '{print $NF}')
        echo "  Deleting service: $SERVICE_NAME"
        
        # Set desired count to 0
        aws ecs update-service \
            --cluster $CLUSTER_NAME \
            --service $SERVICE_NAME \
            --desired-count 0 \
            --region $REGION \
            --no-cli-pager > /dev/null 2>&1
        
        # Force delete
        aws ecs delete-service \
            --cluster $CLUSTER_NAME \
            --service $SERVICE_NAME \
            --force \
            --region $REGION \
            --no-cli-pager > /dev/null 2>&1
        
        echo "  ✅ Deleted $SERVICE_NAME"
    done
    
    # Optionally delete the cluster if empty
    read -p "Delete empty cluster $CLUSTER_NAME? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        aws ecs delete-cluster --cluster $CLUSTER_NAME --region $REGION
        echo "✅ Cluster $CLUSTER_NAME deleted"
    fi
done

# Delete all EventBridge rules with 10am schedule
echo ""
echo "Deleting scheduled EventBridge rules..."

RULES=$(aws events list-rules --region $REGION --query 'Rules[*].Name' --output text)

for RULE in $RULES; do
    SCHEDULE=$(aws events describe-rule --name $RULE --region $REGION --query 'ScheduleExpression' --output text 2>/dev/null)
    
    # Check if it's a 10am cron schedule
    if echo "$SCHEDULE" | grep -q "cron(0 10" || echo "$SCHEDULE" | grep -q "cron(10"; then
        echo "  Deleting rule: $RULE (Schedule: $SCHEDULE)"
        
        # Remove targets
        TARGETS=$(aws events list-targets-by-rule --rule $RULE --region $REGION --query 'Targets[*].Id' --output text)
        
        if [ ! -z "$TARGETS" ]; then
            aws events remove-targets \
                --rule $RULE \
                --ids $(echo $TARGETS) \
                --region $REGION \
                --no-cli-pager > /dev/null 2>&1
        fi
        
        # Delete rule
        aws events delete-rule \
            --name $RULE \
            --region $REGION \
            --no-cli-pager > /dev/null 2>&1
        
        echo "  ✅ Deleted $RULE"
    fi
done

echo ""
echo "========================================="
echo "All ECS services and 10am rules deleted"
echo "========================================="
