#!/bin/bash
# Deployment script for RateMyRickshaw using AWS SAM
# Deploys both Lambda backend and React frontend to AWS

set -e  # Exit on any error

# Configuration
APP_NAME="ratemyrickshaw"
STACK_NAME="ratemyrickshaw"
REGION="eu-west-2"
ENVIRONMENT="prod"  # Can be: dev, staging, prod

echo "========================================="
echo "RateMyRickshaw Deployment"
echo "========================================="
echo "Stack: $STACK_NAME"
echo "Region: $REGION"
echo "Environment: $ENVIRONMENT"
echo ""

# Step 1: Build the Lambda function
echo "Step 1/4: Building Lambda function..."
echo "========================================="
mvn clean package

if [ ! -f "target/function.zip" ]; then
    echo "❌ Error: Lambda build failed - function.zip not found"
    exit 1
fi

echo "✅ Lambda build complete"
echo ""

# Step 2: Deploy infrastructure with SAM
echo "Step 2/4: Deploying infrastructure with SAM..."
echo "========================================="

if [ ! -f "samconfig.toml" ]; then
    echo "Running guided deployment (first time)..."
    sam.cmd deploy \
        --guided \
        --stack-name $STACK_NAME \
        --region $REGION \
        --parameter-overrides Environment=$ENVIRONMENT
else
    echo "Deploying with existing configuration..."
    sam.cmd deploy \
        --stack-name $STACK_NAME \
        --region $REGION \
        --parameter-overrides Environment=$ENVIRONMENT
fi

echo "✅ Infrastructure deployed"
echo ""

# Step 3: Get stack outputs
echo "Step 3/4: Retrieving stack outputs..."
echo "========================================="

# Get API endpoint from CloudFormation outputs
API_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
    --output text 2>/dev/null || echo "")

# Get S3 bucket name
FRONTEND_BUCKET=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`FrontendBucketName`].OutputValue' \
    --output text 2>/dev/null || echo "")

# Get CloudFront distribution ID
CLOUDFRONT_ID=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`CloudFrontDistributionId`].OutputValue' \
    --output text 2>/dev/null || echo "")

if [ -z "$API_ENDPOINT" ]; then
    echo "❌ Error: Could not retrieve API endpoint from stack outputs"
    echo "   Make sure the stack deployed successfully"
    exit 1
fi

if [ -z "$FRONTEND_BUCKET" ]; then
    echo "❌ Error: Could not retrieve frontend bucket name from stack outputs"
    exit 1
fi

echo "API Endpoint: $API_ENDPOINT"
echo "Frontend Bucket: $FRONTEND_BUCKET"
echo ""

# Step 4: Build and deploy frontend
echo "Step 4/4: Building and deploying frontend..."
echo "========================================="

cd frontend

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

# Build frontend with API endpoint
echo "Building React app with API_ENDPOINT=${API_ENDPOINT}/analyze"
REACT_APP_API_ENDPOINT="${API_ENDPOINT}/analyze" npm run build

if [ ! -d "build" ]; then
    echo "❌ Error: Frontend build failed - build directory not found"
    exit 1
fi

# Deploy to S3
echo "Deploying to S3 bucket: $FRONTEND_BUCKET"
aws s3 sync build/ s3://${FRONTEND_BUCKET}/ \
    --delete \
    --region $REGION \
    --cache-control "public, max-age=31536000" \
    --exclude "index.html" \
    --exclude "asset-manifest.json"

# Deploy index.html and manifest with no cache
aws s3 cp build/index.html s3://${FRONTEND_BUCKET}/index.html \
    --region $REGION \
    --cache-control "no-cache, no-store, must-revalidate" \
    --content-type "text/html"

if [ -f "build/asset-manifest.json" ]; then
    aws s3 cp build/asset-manifest.json s3://${FRONTEND_BUCKET}/asset-manifest.json \
        --region $REGION \
        --cache-control "no-cache, no-store, must-revalidate" \
        --content-type "application/json"
fi

echo "✅ Frontend uploaded to S3"

# Invalidate CloudFront cache
if [ -n "$CLOUDFRONT_ID" ]; then
    echo ""
    echo "Invalidating CloudFront cache..."
    INVALIDATION_ID=$(aws cloudfront create-invalidation \
        --distribution-id $CLOUDFRONT_ID \
        --paths "/*" \
        --query 'Invalidation.Id' \
        --output text 2>/dev/null || echo "")
    
    if [ -n "$INVALIDATION_ID" ]; then
        echo "✅ CloudFront invalidation created: $INVALIDATION_ID"
        echo "   Cache will be cleared in 1-2 minutes"
    else
        echo "⚠️  CloudFront invalidation failed - you may need to manually invalidate"
    fi
fi

cd ..

# Get website URL
WEBSITE_URL=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`WebsiteURL`].OutputValue' \
    --output text 2>/dev/null || echo "")

# Get CloudFront domain name
CLOUDFRONT_URL=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`CloudFrontURL`].OutputValue' \
    --output text 2>/dev/null || echo "")

echo ""
echo "========================================="
echo "✅ Deployment Complete!"
echo "========================================="
echo ""
echo "📍 API Endpoint (STABLE - does not change on redeploy):"
echo "   $API_ENDPOINT"
echo ""
echo "🌐 Website URL (HTTPS with custom domain):"
echo "   $WEBSITE_URL"
echo ""
if [ -n "$CLOUDFRONT_URL" ]; then
    echo "☁️  CloudFront Distribution:"
    echo "   https://$CLOUDFRONT_URL"
    echo "   Distribution ID: $CLOUDFRONT_ID"
    echo ""
fi
echo "📦 S3 Bucket:"
echo "   $FRONTEND_BUCKET"
echo ""
echo "🚀 Next steps:"
echo "   1. Open your browser to: $WEBSITE_URL"
echo "   2. Add a CNAME record in your DNS:"
echo "      Name: ratemyrickshaw.snowballsjourney.com"
echo "      Type: CNAME"
echo "      Value: $CLOUDFRONT_URL"
echo "   3. To redeploy Lambda only: mvn clean package && sam.cmd deploy"
echo "   4. To redeploy frontend only:"
echo "      cd frontend && npm run build && aws s3 sync build/ s3://$FRONTEND_BUCKET/ --delete"
echo "      aws cloudfront create-invalidation --distribution-id $CLOUDFRONT_ID --paths \"/*\""
echo ""
echo "💡 Important notes:"
echo "   - The API endpoint URL is STABLE and does not change on redeploy"
echo "   - CloudFront cache takes 1-2 minutes to invalidate after frontend updates"
echo "   - Your site is now served over HTTPS with your custom domain"
echo "   - Make sure to update your DNS CNAME record to point to CloudFront"
echo ""
