#!/bin/bash

# Configuration
APP_NAME="ratemyrickshaw"
REGION="eu-west-2"
LAMBDA_FUNCTION_NAME="${APP_NAME}-lambda"
LAMBDA_ROLE_NAME="${APP_NAME}-lambda-role"

echo "========================================="
echo "Deploying Lambda Function"
echo "========================================="

# Get Lambda role ARN
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
LAMBDA_ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${LAMBDA_ROLE_NAME}"

# Check if function.zip exists
if [ ! -f "target/function.zip" ]; then
    echo "Error: target/function.zip not found!"
    echo "Please run ./build.sh first"
    exit 1
fi

# Check if Lambda function exists
FUNCTION_EXISTS=$(aws lambda get-function --function-name $LAMBDA_FUNCTION_NAME --region $REGION 2>&1)

if echo "$FUNCTION_EXISTS" | grep -q "ResourceNotFoundException"; then
    echo "Creating new Lambda function..."
    
    aws lambda create-function \
      --function-name $LAMBDA_FUNCTION_NAME \
      --runtime java17 \
      --role $LAMBDA_ROLE_ARN \
      --handler io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest \
      --zip-file fileb://target/function.zip \
      --timeout 30 \
      --memory-size 512 \
      --environment Variables="{QUARKUS_LAMBDA_HANDLER=rickshawAnalysis,AWS_REGION=$REGION}" \
      --region $REGION
else
    echo "Updating existing Lambda function..."
    
    aws lambda update-function-code \
      --function-name $LAMBDA_FUNCTION_NAME \
      --zip-file fileb://target/function.zip \
      --region $REGION
    
    sleep 5
    
    aws lambda update-function-configuration \
      --function-name $LAMBDA_FUNCTION_NAME \
      --timeout 30 \
      --memory-size 512 \
      --environment Variables="{QUARKUS_LAMBDA_HANDLER=rickshawAnalysis,AWS_REGION=$REGION}" \
      --region $REGION
fi

echo ""
echo "========================================="
echo "Lambda deployment complete!"
echo "========================================="
echo "Function name: $LAMBDA_FUNCTION_NAME"
echo "Region: $REGION"

# Get function ARN
LAMBDA_ARN=$(aws lambda get-function --function-name $LAMBDA_FUNCTION_NAME --region $REGION --query 'Configuration.FunctionArn' --output text)
echo "Function ARN: $LAMBDA_ARN"
