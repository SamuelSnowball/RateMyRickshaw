#!/bin/bash

# Configuration
APP_NAME="ratemyrickshaw"
REGION="eu-west-2"
LAMBDA_FUNCTION_NAME="${APP_NAME}-lambda"
API_NAME="${APP_NAME}-api"

echo "========================================="
echo "Creating API Gateway"
echo "========================================="

# Get account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Get Lambda ARN
LAMBDA_ARN=$(aws lambda get-function --function-name $LAMBDA_FUNCTION_NAME --region $REGION --query 'Configuration.FunctionArn' --output text)

if [ -z "$LAMBDA_ARN" ]; then
    echo "Error: Lambda function not found!"
    echo "Please run ./deploy-lambda.sh first"
    exit 1
fi

echo "Lambda ARN: $LAMBDA_ARN"

# Create REST API
echo "Creating REST API..."

API_ID=$(aws apigateway create-rest-api \
  --name $API_NAME \
  --description "API for RateMyRickshaw" \
  --region $REGION \
  --endpoint-configuration types=REGIONAL \
  --query 'id' \
  --output text 2>/dev/null)

if [ -z "$API_ID" ]; then
    # API might already exist, try to find it
    API_ID=$(aws apigateway get-rest-apis --region $REGION --query "items[?name=='$API_NAME'].id" --output text)
    
    if [ -z "$API_ID" ]; then
        echo "Error: Failed to create or find API"
        exit 1
    fi
    echo "Using existing API: $API_ID"
else
    echo "Created new API: $API_ID"
fi

# Get root resource ID
ROOT_ID=$(aws apigateway get-resources --rest-api-id $API_ID --region $REGION --query 'items[?path==`/`].id' --output text)

echo "Root resource ID: $ROOT_ID"

# Create /analyze resource
echo "Creating /analyze resource..."

ANALYZE_RESOURCE_ID=$(aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $ROOT_ID \
  --path-part analyze \
  --region $REGION \
  --query 'id' \
  --output text 2>/dev/null || \
  aws apigateway get-resources --rest-api-id $API_ID --region $REGION --query "items[?pathPart=='analyze'].id" --output text)

echo "Analyze resource ID: $ANALYZE_RESOURCE_ID"

# Create POST method
echo "Creating POST method..."

aws apigateway put-method \
  --rest-api-id $API_ID \
  --resource-id $ANALYZE_RESOURCE_ID \
  --http-method POST \
  --authorization-type NONE \
  --region $REGION \
  2>/dev/null || echo "Method may already exist"

# Set up Lambda integration
echo "Setting up Lambda integration..."

aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $ANALYZE_RESOURCE_ID \
  --http-method POST \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri "arn:aws:apigateway:${REGION}:lambda:path/2015-03-31/functions/${LAMBDA_ARN}/invocations" \
  --region $REGION

# Enable CORS on POST method
echo "Enabling CORS..."

aws apigateway put-method \
  --rest-api-id $API_ID \
  --resource-id $ANALYZE_RESOURCE_ID \
  --http-method OPTIONS \
  --authorization-type NONE \
  --region $REGION \
  2>/dev/null || echo "OPTIONS method may already exist"

aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $ANALYZE_RESOURCE_ID \
  --http-method OPTIONS \
  --type MOCK \
  --request-templates '{"application/json": "{\"statusCode\": 200}"}' \
  --region $REGION \
  2>/dev/null

aws apigateway put-method-response \
  --rest-api-id $API_ID \
  --resource-id $ANALYZE_RESOURCE_ID \
  --http-method OPTIONS \
  --status-code 200 \
  --response-parameters '{"method.response.header.Access-Control-Allow-Headers": false, "method.response.header.Access-Control-Allow-Methods": false, "method.response.header.Access-Control-Allow-Origin": false}' \
  --region $REGION \
  2>/dev/null || echo "Method response may already exist"

aws apigateway put-integration-response \
  --rest-api-id $API_ID \
  --resource-id $ANALYZE_RESOURCE_ID \
  --http-method OPTIONS \
  --status-code 200 \
  --response-parameters '{"method.response.header.Access-Control-Allow-Headers": "'"'"'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"'"'", "method.response.header.Access-Control-Allow-Methods": "'"'"'POST,OPTIONS'"'"'", "method.response.header.Access-Control-Allow-Origin": "'"'"'*'"'"'"}' \
  --region $REGION \
  2>/dev/null || echo "Integration response may already exist"

# Grant API Gateway permission to invoke Lambda
echo "Granting API Gateway permission to invoke Lambda..."

STATEMENT_ID="${API_NAME}-apigateway-invoke"

aws lambda add-permission \
  --function-name $LAMBDA_FUNCTION_NAME \
  --statement-id $STATEMENT_ID \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com \
  --source-arn "arn:aws:execute-api:${REGION}:${ACCOUNT_ID}:${API_ID}/*/*" \
  --region $REGION \
  2>/dev/null || echo "Permission may already exist"

# Deploy API
echo "Deploying API to 'prod' stage..."

aws apigateway create-deployment \
  --rest-api-id $API_ID \
  --stage-name prod \
  --description "Production deployment" \
  --region $REGION

API_ENDPOINT="https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod"

echo ""
echo "========================================="
echo "API Gateway deployment complete!"
echo "========================================="
echo "API ID: $API_ID"
echo "API Endpoint: $API_ENDPOINT"
echo "Analyze Endpoint: ${API_ENDPOINT}/analyze"
echo ""
echo "Test with:"
echo "curl -X POST ${API_ENDPOINT}/analyze \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"imageUrl\": \"https://example.com/image.jpg\"}'"
