#!/bin/bash

# Test script for RateMyRickshaw API
# This script tests the deployed Lambda function via API Gateway

APP_NAME="ratemyrickshaw"
REGION="eu-west-2"
API_NAME="${APP_NAME}-api"

echo "========================================="
echo "Testing RateMyRickshaw API"
echo "========================================="

# Get API endpoint
API_ID=$(aws apigateway get-rest-apis --region $REGION --query "items[?name=='$API_NAME'].id" --output text)

if [ -z "$API_ID" ]; then
    echo "❌ Error: API Gateway not found!"
    echo "Please deploy the API first: ./scripts/deploy-api-gateway.sh"
    exit 1
fi

API_ENDPOINT="https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod"

echo "API Endpoint: $API_ENDPOINT"
echo ""

# Test 1: Rickshaw image from URL
echo "Test 1: Analyzing rickshaw image from URL..."
echo "========================================="

RESPONSE=$(curl -s -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Auto_rickshaw_in_Thrissur.jpg/640px-Auto_rickshaw_in_Thrissur.jpg"}')

echo "$RESPONSE" | jq '.' || echo "$RESPONSE"
echo ""

# Test 2: Invalid request (empty request)
echo "Test 2: Testing error handling (empty request)..."
echo "========================================="

RESPONSE=$(curl -s -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{}')

echo "$RESPONSE" | jq '.' || echo "$RESPONSE"
echo ""

# Test 3: Non-rickshaw image
echo "Test 3: Analyzing non-rickshaw image..."
echo "========================================="

RESPONSE=$(curl -s -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/640px-Cat03.jpg"}')

echo "$RESPONSE" | jq '.' || echo "$RESPONSE"
echo ""

echo "========================================="
echo "Testing complete!"
echo "========================================="
