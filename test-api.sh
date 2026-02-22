#!/bin/bash
# Test script for RateMyRickshaw API

STACK_NAME="ratemyrickshaw"
REGION="eu-west-2"

echo "========================================="
echo "Testing RateMyRickshaw API"
echo "========================================="

# Get API endpoint from CloudFormation
API_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
    --output text 2>/dev/null)

if [ -z "$API_ENDPOINT" ]; then
    echo "❌ Error: Could not get API endpoint from stack"
    echo "Please deploy first: ./deploy-sam.sh"
    exit 1
fi

echo "API Endpoint: $API_ENDPOINT/analyze"
echo ""

# Test 1: Rickshaw image from URL
echo "Test 1: Analyzing rickshaw image from URL..."
echo "========================================="

RESPONSE=$(curl -s -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Auto_rickshaw_in_Thrissur.jpg/640px-Auto_rickshaw_in_Thrissur.jpg"}')

echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

# Test 2: Invalid request (empty request)
echo "Test 2: Testing error handling (empty request)..."
echo "========================================="

RESPONSE=$(curl -s -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{}')

echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

# Test 3: Non-rickshaw image
echo "Test 3: Analyzing non-rickshaw image..."
echo "========================================="

RESPONSE=$(curl -s -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/640px-Cat03.jpg"}')

echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

echo "========================================="
echo "✅ API testing complete"
echo "========================================="
