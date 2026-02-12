#!/bin/bash

# Master deployment script for RateMyRickshaw
# This script orchestrates the entire deployment process

set -e  # Exit on any error

APP_NAME="ratemyrickshaw"
REGION="eu-west-2"

echo "========================================="
echo "RateMyRickshaw Full Deployment"
echo "========================================="
echo "Region: $REGION"
echo "App Name: $APP_NAME"
echo ""

# Step 1: Build the Lambda function
echo "Step 1/5: Building Lambda function..."
echo "========================================="
if [ -f "build.sh" ]; then
    chmod +x build.sh
    ./build.sh
else
    ./mvnw clean package -DskipTests
fi

if [ ! -f "target/function.zip" ]; then
    echo "Error: Lambda build failed - function.zip not found"
    exit 1
fi

echo ""
echo "✅ Lambda build complete"
echo ""

# Step 2: Setup infrastructure
echo "Step 2/5: Setting up AWS infrastructure..."
echo "========================================="
chmod +x scripts/setup-infrastructure.sh
./scripts/setup-infrastructure.sh

echo ""
echo "✅ Infrastructure setup complete"
echo ""

# Step 3: Deploy Lambda
echo "Step 3/5: Deploying Lambda function..."
echo "========================================="
chmod +x scripts/deploy-lambda.sh
./scripts/deploy-lambda.sh

echo ""
echo "✅ Lambda deployment complete"
echo ""

# Step 4: Deploy API Gateway
echo "Step 4/5: Deploying API Gateway..."
echo "========================================="
chmod +x scripts/deploy-api-gateway.sh
./scripts/deploy-api-gateway.sh

echo ""
echo "✅ API Gateway deployment complete"
echo ""

# Step 5: Deploy Frontend
echo "Step 5/5: Deploying frontend..."
echo "========================================="
chmod +x scripts/deploy-frontend.sh
./scripts/deploy-frontend.sh

echo ""
echo "✅ Frontend deployment complete"
echo ""

# Get final URLs
API_ID=$(aws apigateway get-rest-apis --region $REGION --query "items[?name=='${APP_NAME}-api'].id" --output text)
API_ENDPOINT="https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod"
WEBSITE_URL="http://${APP_NAME}.s3-website.${REGION}.amazonaws.com"

echo ""
echo "========================================="
echo "🎉 DEPLOYMENT COMPLETE!"
echo "========================================="
echo ""
echo "📍 Your application is now live!"
echo ""
echo "🌐 Website URL:"
echo "   $WEBSITE_URL"
echo ""
echo "🔗 API Endpoint:"
echo "   $API_ENDPOINT"
echo ""
echo "📝 Test your API:"
echo "   curl -X POST $API_ENDPOINT/analyze \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"imageUrl\": \"https://example.com/rickshaw.jpg\"}'"
echo ""
echo "========================================="
