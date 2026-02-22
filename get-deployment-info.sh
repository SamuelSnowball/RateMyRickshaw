#!/bin/bash
# Get deployment information for RateMyRickshaw

STACK_NAME="ratemyrickshaw"
REGION="eu-west-2"

echo "========================================="
echo "RateMyRickshaw Deployment Info"
echo "========================================="
echo ""

# Check if stack exists
STACK_STATUS=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].StackStatus' \
    --output text 2>/dev/null)

if [ -z "$STACK_STATUS" ]; then
    echo "❌ Stack not deployed"
    echo "Run: ./deploy-sam.sh"
    exit 1
fi

echo "✅ Stack Status: $STACK_STATUS"
echo ""

# Get outputs
echo "Stack Outputs:"
echo "-------------------"
aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue]' \
    --output table

echo ""
echo "Resources:"
echo "-------------------"
aws cloudformation describe-stack-resources \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'StackResources[*].[LogicalResourceId,ResourceType,ResourceStatus]' \
    --output table
