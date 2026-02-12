# QuickStart Guide - RateMyRickshaw

## Prerequisites Check

Before starting, ensure you have:

```bash
# Check Java version (need 17+)
java -version

# Check Maven (need 3.8+)
mvn -version

# Check Node.js (need 16+)
node -version

# Check AWS CLI
aws --version

# Check AWS credentials
aws sts get-caller-identity
```

## First Time Setup

### 1. Configure AWS Credentials

```bash
aws configure
```

Enter:
- AWS Access Key ID
- AWS Secret Access Key
- Default region: `eu-west-2`
- Default output format: `json`

### 2. Test AWS Permissions

```bash
# Test IAM permissions
aws iam list-roles --max-items 1

# Test Lambda permissions
aws lambda list-functions --max-items 1 --region eu-west-2

# Test S3 permissions
aws s3 ls

# Test Rekognition permissions
aws rekognition list-collections --region eu-west-2
```

## Deployment Options

### Option A: Full Auto Deploy (Recommended for first time)

```bash
# Make script executable
chmod +x deploy.sh

# Run full deployment
./deploy.sh
```

This will:
1. Build the Lambda function
2. Create AWS infrastructure (IAM roles, S3 bucket)
3. Deploy Lambda function
4. Create and configure API Gateway
5. Build and deploy React frontend

Total time: ~5-10 minutes

### Option B: Manual Step-by-Step

Useful for understanding the process or debugging.

#### Step 1: Build Lambda (30 seconds)

```bash
chmod +x build.sh
./build.sh
```

Verify: Check `target/function.zip` exists

#### Step 2: Setup Infrastructure (1 minute)

```bash
chmod +x scripts/setup-infrastructure.sh
./scripts/setup-infrastructure.sh
```

This creates:
- IAM role: `ratemyrickshaw-lambda-role`
- S3 bucket: `ratemyrickshaw`

Verify in AWS Console:
- IAM > Roles > ratemyrickshaw-lambda-role
- S3 > Buckets > ratemyrickshaw

#### Step 3: Deploy Lambda (1 minute)

```bash
chmod +x scripts/deploy-lambda.sh
./scripts/deploy-lambda.sh
```

Verify: 
```bash
aws lambda get-function --function-name ratemyrickshaw-lambda --region eu-west-2
```

#### Step 4: Deploy API Gateway (2 minutes)

```bash
chmod +x scripts/deploy-api-gateway.sh
./scripts/deploy-api-gateway.sh
```

Note the API endpoint URL printed at the end.

Verify:
```bash
# Test the API (replace with your endpoint)
curl -X POST https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://example.com/test.jpg"}'
```

#### Step 5: Deploy Frontend (2-3 minutes)

```bash
cd frontend
npm install
cd ..

chmod +x scripts/deploy-frontend.sh
./scripts/deploy-frontend.sh
```

Note the Website URL printed at the end.

## Testing Your Deployment

### Test the API

```bash
# Get your API endpoint
API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)
API_ENDPOINT="https://${API_ID}.execute-api.eu-west-2.amazonaws.com/prod"

# Test with a public rickshaw image
curl -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Auto_rickshaw_in_Thrissur.jpg/640px-Auto_rickshaw_in_Thrissur.jpg"
  }'
```

Expected response:
```json
{
  "success": true,
  "message": "Image analyzed successfully",
  "isRickshaw": true,
  "rickshawConfidence": 90.0,
  "labels": ["Vehicle", "Transportation", "Auto Rickshaw", ...],
  "labelConfidence": { ... }
}
```

### Test the Website

```bash
# Get website URL
WEBSITE_URL="http://ratemyrickshaw.s3-website.eu-west-2.amazonaws.com"
echo "Open your browser to: $WEBSITE_URL"
```

Or simply open: http://ratemyrickshaw.s3-website.eu-west-2.amazonaws.com

## Local Development

### Run Lambda Locally

```bash
# Terminal 1: Start Quarkus dev mode
./mvnw quarkus:dev

# Terminal 2: Test locally
curl -X POST http://localhost:8080/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://example.com/rickshaw.jpg"}'
```

### Run Frontend Locally

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start dev server
REACT_APP_API_ENDPOINT=http://localhost:8080 npm start
```

Opens automatically at http://localhost:3000

## Common Issues

### Issue: "Access Denied" during S3 bucket creation

**Solution**: Bucket name must be globally unique. Edit scripts and change:
```bash
APP_NAME="ratemyrickshaw-yourname-12345"
```

### Issue: "Role not found" when creating Lambda

**Solution**: Wait 10-15 seconds for IAM role to propagate, then retry.

### Issue: Maven build fails

**Solution**: 
```bash
# Clean and retry
./mvnw clean
./mvnw package -DskipTests
```

### Issue: Frontend shows "Configuration Required"

**Solution**: Redeploy frontend to pick up the API endpoint:
```bash
./scripts/deploy-frontend.sh
```

### Issue: CORS errors in browser

**Solution**: Redeploy API Gateway:
```bash
./scripts/deploy-api-gateway.sh
```

## What's Next?

### Customize for Your Needs

1. **Change detection keywords**: Edit [RekognitionService.java](src/main/java/com/ratemyrickshaw/service/RekognitionService.java):
   ```java
   Set<String> rickshawKeywords = Set.of(
       "your", "custom", "keywords"
   );
   ```

2. **Adjust confidence threshold**: Edit same file:
   ```java
   .minConfidence(70F)  // Change from 70 to your preferred value
   ```

3. **Update UI styling**: Edit [App.css](frontend/src/App.css)

### Add Features

- Image upload from local file
- Save results to DynamoDB
- User authentication with Cognito
- CloudFront CDN for faster frontend
- CI/CD with GitHub Actions

## Monitoring

### View Lambda Logs

```bash
# Get latest log stream
LOG_STREAM=$(aws logs describe-log-streams \
  --log-group-name /aws/lambda/ratemyrickshaw-lambda \
  --region eu-west-2 \
  --order-by LastEventTime \
  --descending \
  --max-items 1 \
  --query 'logStreams[0].logStreamName' \
  --output text)

# View logs
aws logs get-log-events \
  --log-group-name /aws/lambda/ratemyrickshaw-lambda \
  --log-stream-name "$LOG_STREAM" \
  --region eu-west-2
```

### View API Gateway Logs

Enable in AWS Console:
1. API Gateway > Your API > Stages > prod
2. Logs/Tracing tab
3. Enable CloudWatch Logs

## Costs Estimate

For 10,000 requests/month:
- Lambda: $0.20
- API Gateway: $0.35
- Rekognition: $10 (1,000 images)
- S3: $0.50
- Data Transfer: $0.50

**Total**: ~$12/month

Free tier (first 12 months):
- Lambda: 1M requests/month free
- Rekognition: 5,000 images/month free
- S3: 5GB storage free

## Cleanup

When done testing:

```bash
# Delete all resources
aws lambda delete-function --function-name ratemyrickshaw-lambda --region eu-west-2

API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)
aws apigateway delete-rest-api --rest-api-id $API_ID --region eu-west-2

aws s3 rm s3://ratemyrickshaw --recursive
aws s3 rb s3://ratemyrickshaw

aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/AmazonRekognitionReadOnlyAccess
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
aws iam delete-role --role-name ratemyrickshaw-lambda-role
```

## Getting Help

- Check logs in CloudWatch
- Review AWS Console for resource status
- See detailed README.md for architecture details
- AWS Documentation: https://docs.aws.amazon.com/

Enjoy building with RateMyRickshaw! 🛺
