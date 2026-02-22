#!/bin/bash
# Simplified deployment script for RateMyRickshaw using SAM

set -e  # Exit on any error

APP_NAME="ratemyrickshaw"
REGION="eu-west-2"

echo "========================================="
echo "RateMyRickshaw Deployment"
echo "========================================="
echo "Region: $REGION"
echo ""

# Step 1: Build the Lambda function
echo "Step 1/3: Building Lambda function..."
echo "========================================="
mvn clean package

if [ ! -f "target/function.zip" ]; then
    echo "Error: Lambda build failed - function.zip not found"
    exit 1
fi

echo "✅ Lambda build complete"
echo ""

# Step 2: Deploy with SAM
echo "Step 2/3: Deploying with SAM..."
echo "========================================="

if [ ! -f "samconfig.toml" ]; then
    echo "Running guided deployment (first time)..."
    sam deploy --guided --region $REGION
else
    echo "Deploying with existing configuration..."
    sam deploy
fi

echo "✅ Infrastructure deployed"
echo ""

# Step 3: Deploy Frontend
echo "Step 3/3: Deploying Frontend..."
echo "========================================="

# Get API endpoint from CloudFormation outputs
API_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name ratemyrickshaw \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
    --output text 2>/dev/null || echo "")

if [ -z "$API_ENDPOINT" ]; then
    echo "Warning: Could not get API endpoint from stack outputs"
    echo "You may need to manually configure REACT_APP_API_ENDPOINT"
    API_ENDPOINT="YOUR_API_ENDPOINT"
fi

echo "API Endpoint: $API_ENDPOINT"

# Build frontend
cd frontend
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

echo "Building frontend..."
REACT_APP_API_ENDPOINT=$API_ENDPOINT npm run build

# Deploy to S3
echo "Deploying to S3..."
aws s3 sync build/ s3://${APP_NAME}/ --delete --region $REGION

cd ..

# Get website URL
WEBSITE_URL=$(aws cloudformation describe-stacks \
    --stack-name ratemyrickshaw \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`WebsiteURL`].OutputValue' \
    --output text 2>/dev/null || echo "http://${APP_NAME}.s3-website.${REGION}.amazonaws.com")

echo ""
echo "========================================="
echo "✅ Deployment Complete!"
echo "========================================="
echo "API Endpoint: $API_ENDPOINT"
echo "Website URL: $WEBSITE_URL"
echo ""
echo "Open your browser to: $WEBSITE_URL"
