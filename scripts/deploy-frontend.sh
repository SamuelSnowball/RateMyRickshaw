#!/bin/bash

# Configuration
APP_NAME="ratemyrickshaw"
REGION="eu-west-2"
S3_BUCKET_NAME="${APP_NAME}"
API_NAME="${APP_NAME}-api"

echo "========================================="
echo "Deploying Frontend to S3"
echo "========================================="

# Change to frontend directory
cd frontend

# Get API Gateway endpoint
API_ID=$(aws apigateway get-rest-apis --region $REGION --query "items[?name=='$API_NAME'].id" --output text)

if [ -z "$API_ID" ]; then
    echo "Warning: API Gateway not found. The frontend will need manual configuration."
    API_ENDPOINT="YOUR_API_GATEWAY_ENDPOINT_HERE"
else
    API_ENDPOINT="https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod"
    echo "Found API endpoint: $API_ENDPOINT"
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Build the React app with API endpoint
echo "Building React app..."
REACT_APP_API_ENDPOINT=$API_ENDPOINT npm run build

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Deploy to S3
echo "Deploying to S3 bucket: $S3_BUCKET_NAME"

aws s3 sync build/ s3://$S3_BUCKET_NAME/ \
  --delete \
  --region $REGION

# Invalidate CloudFront cache if distribution exists (optional)
# DISTRIBUTION_ID=$(aws cloudfront list-distributions --query "DistributionList.Items[?Origins.Items[?DomainName=='${S3_BUCKET_NAME}.s3.amazonaws.com']].Id" --output text)
# if [ ! -z "$DISTRIBUTION_ID" ]; then
#     echo "Invalidating CloudFront cache..."
#     aws cloudfront create-invalidation --distribution-id $DISTRIBUTION_ID --paths "/*"
# fi

WEBSITE_URL="http://${S3_BUCKET_NAME}.s3-website.${REGION}.amazonaws.com"

echo ""
echo "========================================="
echo "Frontend deployment complete!"
echo "========================================="
echo "Website URL: $WEBSITE_URL"
echo "API Endpoint: $API_ENDPOINT"
echo ""
echo "Open your browser to: $WEBSITE_URL"

cd ..
