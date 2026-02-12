#!/bin/bash

# Configuration
APP_NAME="ratemyrickshaw"
REGION="eu-west-2"
LAMBDA_FUNCTION_NAME="${APP_NAME}-lambda"
LAMBDA_ROLE_NAME="${APP_NAME}-lambda-role"
S3_BUCKET_NAME="${APP_NAME}"
API_NAME="${APP_NAME}-api"

echo "========================================="
echo "RateMyRickshaw AWS Infrastructure Setup"
echo "========================================="
echo "Region: $REGION"
echo "App Name: $APP_NAME"
echo ""

# Create IAM role for Lambda
echo "Creating IAM role for Lambda..."

TRUST_POLICY='{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}'

aws iam create-role \
  --role-name $LAMBDA_ROLE_NAME \
  --assume-role-policy-document "$TRUST_POLICY" \
  --region $REGION \
  2>/dev/null || echo "Role already exists"

# Attach policies to Lambda role
echo "Attaching policies to Lambda role..."

aws iam attach-role-policy \
  --role-name $LAMBDA_ROLE_NAME \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole \
  --region $REGION

aws iam attach-role-policy \
  --role-name $LAMBDA_ROLE_NAME \
  --policy-arn arn:aws:iam::aws:policy/AmazonRekognitionReadOnlyAccess \
  --region $REGION

aws iam attach-role-policy \
  --role-name $LAMBDA_ROLE_NAME \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess \
  --region $REGION

# Get account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
LAMBDA_ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${LAMBDA_ROLE_NAME}"

echo "Lambda Role ARN: $LAMBDA_ROLE_ARN"
echo "Waiting for IAM role to propagate..."
sleep 10

# Create S3 bucket for static website
echo "Creating S3 bucket for static website..."

aws s3 mb s3://$S3_BUCKET_NAME --region $REGION 2>/dev/null || echo "Bucket may already exist"

# Configure S3 bucket for static website hosting
echo "Configuring S3 bucket for static website hosting..."

aws s3 website s3://$S3_BUCKET_NAME \
  --index-document index.html \
  --error-document index.html

# Make S3 bucket public
BUCKET_POLICY=$(cat <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::BUCKET_NAME/*"
    }
  ]
}
EOF
)

BUCKET_POLICY="${BUCKET_POLICY//BUCKET_NAME/$S3_BUCKET_NAME}"

echo "$BUCKET_POLICY" | aws s3api put-bucket-policy \
  --bucket $S3_BUCKET_NAME \
  --policy file:///dev/stdin

# Disable block public access
aws s3api put-public-access-block \
  --bucket $S3_BUCKET_NAME \
  --public-access-block-configuration "BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false"

echo ""
echo "========================================="
echo "Infrastructure setup complete!"
echo "========================================="
echo "S3 Bucket: $S3_BUCKET_NAME"
echo "S3 Website URL: http://${S3_BUCKET_NAME}.s3-website.${REGION}.amazonaws.com"
echo "Lambda Role ARN: $LAMBDA_ROLE_ARN"
echo ""
echo "Next steps:"
echo "1. Run ./deploy-lambda.sh to deploy the Lambda function"
echo "2. Run ./deploy-api-gateway.sh to create API Gateway"
echo "3. Run ./deploy-frontend.sh to deploy the React UI"
