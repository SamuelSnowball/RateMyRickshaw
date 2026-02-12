# Quick Reference - Common Commands

## 🚀 Deployment

### Full Deployment
```bash
./deploy.sh
```

### Individual Steps
```bash
./build.sh                              # Build Lambda
./scripts/setup-infrastructure.sh       # Setup AWS resources
./scripts/deploy-lambda.sh              # Deploy Lambda
./scripts/deploy-api-gateway.sh         # Deploy API Gateway
./scripts/deploy-frontend.sh            # Deploy React app
```

## 🧪 Testing

### Check Deployment Status
```bash
./scripts/get-info.sh
```

### Test API
```bash
./scripts/test-api.sh
```

### Manual API Test
```bash
# Get API ID first
API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)

# Test endpoint
curl -X POST https://${API_ID}.execute-api.eu-west-2.amazonaws.com/prod/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://example.com/rickshaw.jpg"}'
```

## 💻 Local Development

### Run Backend Locally
```bash
./mvnw quarkus:dev
# Access at http://localhost:8080
```

### Run Frontend Locally
```bash
cd frontend
npm install
REACT_APP_API_ENDPOINT=http://localhost:8080 npm start
# Access at http://localhost:3000
```

## 📊 Monitoring

### View Lambda Logs
```bash
# Tail logs in real-time
aws logs tail /aws/lambda/ratemyrickshaw-lambda --follow --region eu-west-2

# Get recent logs
aws logs tail /aws/lambda/ratemyrickshaw-lambda --since 1h --region eu-west-2
```

### Check Lambda Metrics
```bash
# Get invocation count
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=ratemyrickshaw-lambda \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Sum \
  --region eu-west-2
```

## 🔧 Configuration

### Update Lambda Code
```bash
./build.sh
./scripts/deploy-lambda.sh
```

### Update Frontend
```bash
cd frontend
npm run build
cd ..
./scripts/deploy-frontend.sh
```

### Update API Gateway
```bash
./scripts/deploy-api-gateway.sh
```

## 🧹 Cleanup

### Delete Everything
```bash
# Lambda
aws lambda delete-function --function-name ratemyrickshaw-lambda --region eu-west-2

# API Gateway
API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)
aws apigateway delete-rest-api --rest-api-id $API_ID --region eu-west-2

# S3
aws s3 rm s3://ratemyrickshaw --recursive
aws s3 rb s3://ratemyrickshaw

# IAM
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/AmazonRekognitionReadOnlyAccess
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
aws iam delete-role --role-name ratemyrickshaw-lambda-role
```

## 🔍 Debugging

### Test Lambda Directly
```bash
# Invoke Lambda function
aws lambda invoke \
  --function-name ratemyrickshaw-lambda \
  --payload '{"imageUrl":"https://example.com/test.jpg"}' \
  --region eu-west-2 \
  output.json

# View response
cat output.json | jq '.'
```

### Check API Gateway Integration
```bash
# Get API details
API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)

# List resources
aws apigateway get-resources --rest-api-id $API_ID --region eu-west-2

# Get integration
RESOURCE_ID=$(aws apigateway get-resources --rest-api-id $API_ID --region eu-west-2 --query "items[?path=='/analyze'].id" --output text)
aws apigateway get-integration --rest-api-id $API_ID --resource-id $RESOURCE_ID --http-method POST --region eu-west-2
```

### Verify Permissions
```bash
# Check Lambda role
aws iam get-role --role-name ratemyrickshaw-lambda-role

# List attached policies
aws iam list-attached-role-policies --role-name ratemyrickshaw-lambda-role

# Get Lambda policy
aws lambda get-policy --function-name ratemyrickshaw-lambda --region eu-west-2
```

## 📝 Useful AWS CLI Commands

### List Resources
```bash
# List all Lambda functions
aws lambda list-functions --region eu-west-2

# List all APIs
aws apigateway get-rest-apis --region eu-west-2

# List S3 buckets
aws s3 ls

# List IAM roles
aws iam list-roles --query 'Roles[?contains(RoleName, `ratemyrickshaw`)]'
```

### Get Resource ARNs
```bash
# Lambda ARN
aws lambda get-function --function-name ratemyrickshaw-lambda --region eu-west-2 --query 'Configuration.FunctionArn'

# IAM Role ARN
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "arn:aws:iam::${ACCOUNT_ID}:role/ratemyrickshaw-lambda-role"
```

## 🌐 URLs

### Get All URLs
```bash
# API Endpoint
API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)
echo "API: https://${API_ID}.execute-api.eu-west-2.amazonaws.com/prod"

# Website URL
echo "Website: http://ratemyrickshaw.s3-website.eu-west-2.amazonaws.com"

# Lambda Console
echo "Lambda: https://eu-west-2.console.aws.amazon.com/lambda/home?region=eu-west-2#/functions/ratemyrickshaw-lambda"
```

## 🔐 Security

### Rotate API Key (if implemented)
```bash
# Create usage plan (if not exists)
aws apigateway create-usage-plan --name ratemyrickshaw-plan --region eu-west-2

# Create API key
aws apigateway create-api-key --name ratemyrickshaw-key --enabled --region eu-west-2
```

### Enable CloudTrail
```bash
# Create trail
aws cloudtrail create-trail --name ratemyrickshaw-trail --s3-bucket-name my-cloudtrail-bucket

# Start logging
aws cloudtrail start-logging --name ratemyrickshaw-trail
```

## 📦 Build & Package

### Maven Commands
```bash
# Clean build
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Run tests только
./mvnw test

# Run specific test
./mvnw test -Dtest=RickshawAnalysisHandlerTest
```

### Frontend Commands
```bash
cd frontend

# Install dependencies
npm install

# Development server
npm start

# Production build
npm run build

# Run tests
npm test
```

## 🎯 Quick Tests

### Test with Sample Images
```bash
API_ENDPOINT="https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod"

# Test rickshaw
curl -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Auto_rickshaw_in_Thrissur.jpg/640px-Auto_rickshaw_in_Thrissur.jpg"}'

# Test non-rickshaw (cat)
curl -X POST $API_ENDPOINT/analyze \
  -H 'Content-Type: application/json' \
  -d '{"imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/640px-Cat03.jpg"}'
```

## 💡 Tips

- Use `jq` for JSON formatting: `aws ... | jq '.'`
- Use `--query` to filter AWS CLI output
- Use `--output text` for scriptable output
- Use `--debug` to troubleshoot AWS CLI issues
- Check CloudWatch Logs for Lambda errors
- Enable API Gateway logging for debugging

## 📚 Documentation Links

- [AWS Lambda](https://docs.aws.amazon.com/lambda/)
- [AWS Rekognition](https://docs.aws.amazon.com/rekognition/)
- [AWS API Gateway](https://docs.aws.amazon.com/apigateway/)
- [Quarkus](https://quarkus.io/guides/)
- [React](https://react.dev/)
