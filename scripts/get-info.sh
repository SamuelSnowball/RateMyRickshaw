#!/bin/bash

# Utility script to get deployment information

APP_NAME="ratemyrickshaw"
REGION="eu-west-2"

echo "========================================="
echo "RateMyRickshaw Deployment Info"
echo "========================================="
echo ""

# Check if AWS CLI is configured
if ! aws sts get-caller-identity &>/dev/null; then
    echo "❌ AWS CLI not configured or credentials invalid"
    echo "Run: aws configure"
    exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "✅ AWS Account ID: $ACCOUNT_ID"
echo "✅ Region: $REGION"
echo ""

# Check Lambda function
echo "Lambda Function:"
echo "-------------------"
LAMBDA_EXISTS=$(aws lambda get-function --function-name ${APP_NAME}-lambda --region $REGION 2>&1)
if echo "$LAMBDA_EXISTS" | grep -q "ResourceNotFoundException"; then
    echo "❌ Not deployed"
else
    LAMBDA_ARN=$(echo "$LAMBDA_EXISTS" | jq -r '.Configuration.FunctionArn')
    LAMBDA_RUNTIME=$(echo "$LAMBDA_EXISTS" | jq -r '.Configuration.Runtime')
    LAMBDA_MEMORY=$(echo "$LAMBDA_EXISTS" | jq -r '.Configuration.MemorySize')
    LAMBDA_TIMEOUT=$(echo "$LAMBDA_EXISTS" | jq -r '.Configuration.Timeout')
    echo "✅ Deployed"
    echo "  ARN: $LAMBDA_ARN"
    echo "  Runtime: $LAMBDA_RUNTIME"
    echo "  Memory: ${LAMBDA_MEMORY}MB"
    echo "  Timeout: ${LAMBDA_TIMEOUT}s"
fi
echo ""

# Check API Gateway
echo "API Gateway:"
echo "-------------------"
API_ID=$(aws apigateway get-rest-apis --region $REGION --query "items[?name=='${APP_NAME}-api'].id" --output text 2>/dev/null)
if [ -z "$API_ID" ]; then
    echo "❌ Not deployed"
else
    API_ENDPOINT="https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod"
    echo "✅ Deployed"
    echo "  API ID: $API_ID"
    echo "  Endpoint: $API_ENDPOINT"
    echo "  Analyze: ${API_ENDPOINT}/analyze"
fi
echo ""

# Check S3 bucket
echo "S3 Bucket:"
echo "-------------------"
BUCKET_EXISTS=$(aws s3 ls s3://${APP_NAME} 2>&1)
if echo "$BUCKET_EXISTS" | grep -q "NoSuchBucket"; then
    echo "❌ Not created"
else
    WEBSITE_URL="http://${APP_NAME}.s3-website.${REGION}.amazonaws.com"
    echo "✅ Created"
    echo "  Bucket: ${APP_NAME}"
    echo "  Website URL: $WEBSITE_URL"
fi
echo ""

# Check IAM role
echo "IAM Role:"
echo "-------------------"
ROLE_EXISTS=$(aws iam get-role --role-name ${APP_NAME}-lambda-role 2>&1)
if echo "$ROLE_EXISTS" | grep -q "NoSuchEntity"; then
    echo "❌ Not created"
else
    ROLE_ARN=$(echo "$ROLE_EXISTS" | jq -r '.Role.Arn')
    echo "✅ Created"
    echo "  ARN: $ROLE_ARN"
fi
echo ""

# Summary
echo "========================================="
echo "Quick Links:"
echo "========================================="

if [ ! -z "$API_ENDPOINT" ]; then
    echo "🔗 Test API:"
    echo "   curl -X POST $API_ENDPOINT/analyze \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"imageUrl\": \"https://example.com/image.jpg\"}'"
    echo ""
fi

if [ ! -z "$WEBSITE_URL" ]; then
    echo "🌐 Website:"
    echo "   $WEBSITE_URL"
    echo ""
fi

echo "📊 AWS Console Links:"
echo "   Lambda: https://console.aws.amazon.com/lambda/home?region=${REGION}#/functions/${APP_NAME}-lambda"
echo "   API Gateway: https://console.aws.amazon.com/apigateway/home?region=${REGION}#/apis"
echo "   S3: https://s3.console.aws.amazon.com/s3/buckets/${APP_NAME}"
echo ""

# Check if everything is deployed
if [ ! -z "$API_ID" ] && [ ! -z "$LAMBDA_ARN" ] && [ -z "$(echo "$BUCKET_EXISTS" | grep -q "NoSuchBucket")" ]; then
    echo "✅ All components deployed successfully!"
else
    echo "⚠️  Some components are missing. Run ./deploy.sh to deploy."
fi
