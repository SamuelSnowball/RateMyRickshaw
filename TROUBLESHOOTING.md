# Troubleshooting Guide

## Common Issues and Solutions

### 1. JSON Escape Error: "Bad Unicode escape in JSON"

**Error Message:**
```
Bad Unicode escape in JSON at position 3452 (line 1 column 3453)
```

**Cause:** This occurs when shell scripts contain improperly escaped JSON strings, especially in environment variables or request templates.

**Solutions:**

#### A. Use Heredoc for Complex JSON

Instead of:
```bash
aws apigateway put-integration \
  --request-templates '{"application/json": "{\"statusCode\": 200}"}'
```

Use:
```bash
REQUEST_TEMPLATE=$(cat <<'EOF'
{"application/json": "{\"statusCode\": 200}"}
EOF
)

aws apigateway put-integration \
  --request-templates "$REQUEST_TEMPLATE"
```

#### B. Use Single Quotes for Outer String

```bash
# Good
aws lambda update-function-configuration \
  --environment Variables='{QUARKUS_LAMBDA_HANDLER=rickshawAnalysis,AWS_REGION=eu-west-2}'

# Bad - double quotes can cause issues
aws lambda update-function-configuration \
  --environment Variables="{QUARKUS_LAMBDA_HANDLER=rickshawAnalysis,AWS_REGION=eu-west-2}"
```

#### C. Use JSON Files

Instead of inline JSON:
```bash
# Create a file
cat > integration-request.json <<EOF
{
  "application/json": "{\"statusCode\": 200}"
}
EOF

# Use file
aws apigateway put-integration \
  --request-templates file://integration-request.json
```

### 2. "Access Denied" Creating S3 Bucket

**Cause:** Bucket name already exists (globally) or insufficient permissions.

**Solutions:**

1. **Use a unique bucket name:**
   ```bash
   # Edit all deployment scripts
   S3_BUCKET_NAME="ratemyrickshaw-yourname-$(date +%s)"
   ```

2. **Check AWS permissions:**
   ```bash
   aws s3api list-buckets
   ```

3. **Try a different region:**
   ```bash
   REGION="us-east-1"  # Change in all scripts
   ```

### 3. Lambda Function Timeout

**Error:** Task timed out after 30.00 seconds

**Solutions:**

1. **Increase timeout in deployment script:**
   ```bash
   # Edit scripts/deploy-lambda.sh
   --timeout 60  # Change from 30 to 60
   ```

2. **Increase memory (more memory = more CPU):**
   ```bash
   --memory-size 1024  # Change from 512
   ```

3. **Check Rekognition region:**
   Ensure Lambda and Rekognition are in the same region.

### 4. CORS Errors in Frontend

**Error:** Access to fetch has been blocked by CORS policy

**Solutions:**

1. **Redeploy API Gateway:**
   ```bash
   ./scripts/deploy-api-gateway.sh
   ```

2. **Manually configure CORS in AWS Console:**
   - API Gateway → Your API → Resources
   - Select /analyze → Enable CORS
   - Deploy API to prod stage

3. **Verify CORS headers:**
   ```bash
   curl -X OPTIONS https://YOUR_API/prod/analyze \
     -H "Origin: http://ratemyrickshaw.s3-website.eu-west-2.amazonaws.com" \
     -v
   ```

### 5. "Role Not Found" Error

**Error:** The role defined for the function cannot be assumed by Lambda

**Cause:** IAM role not yet propagated.

**Solution:**

1. **Wait and retry:**
   ```bash
   sleep 15
   ./scripts/deploy-lambda.sh
   ```

2. **Verify role exists:**
   ```bash
   aws iam get-role --role-name ratemyrickshaw-lambda-role
   ```

3. **Recreate role:**
   ```bash
   ./scripts/setup-infrastructure.sh
   ```

### 6. Maven Build Fails

**Error:** Failed to execute goal... compilation failure

**Solutions:**

1. **Check Java version:**
   ```bash
   java -version  # Should be 17 or higher
   ```

2. **Clean and rebuild:**
   ```bash
   ./mvnw clean
   ./mvnw package -DskipTests -X  # -X for debug output
   ```

3. **Download Maven wrapper:**
   ```bash
   mvn wrapper:wrapper
   ```

### 7. Frontend Shows "Configuration Required"

**Cause:** API endpoint not set in React app.

**Solutions:**

1. **Automatic (preferred):**
   ```bash
   ./scripts/deploy-frontend.sh
   ```
   This automatically sets the endpoint.

2. **Manual:**
   ```bash
   # Get API ID
   API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)
   
   # Build with endpoint
   cd frontend
   REACT_APP_API_ENDPOINT=https://${API_ID}.execute-api.eu-west-2.amazonaws.com/prod npm run build
   
   # Deploy
   aws s3 sync build/ s3://ratemyrickshaw/ --delete
   ```

### 8. API Returns 502 Bad Gateway

**Causes:** Lambda error, timeout, or misconfiguration.

**Solutions:**

1. **Check Lambda logs:**
   ```bash
   aws logs tail /aws/lambda/ratemyrickshaw-lambda --follow --region eu-west-2
   ```

2. **Test Lambda directly:**
   ```bash
   aws lambda invoke \
     --function-name ratemyrickshaw-lambda \
     --payload '{"imageUrl":"https://example.com/test.jpg"}' \
     --region eu-west-2 \
     response.json
   
   cat response.json
   ```

3. **Check Lambda permissions:**
   ```bash
   aws lambda get-policy \
     --function-name ratemyrickshaw-lambda \
     --region eu-west-2
   ```

### 9. Rekognition Access Denied

**Error:** User is not authorized to perform: rekognition:DetectLabels

**Solutions:**

1. **Attach Rekognition policy to Lambda role:**
   ```bash
   aws iam attach-role-policy \
     --role-name ratemyrickshaw-lambda-role \
     --policy-arn arn:aws:iam::aws:policy/AmazonRekognitionReadOnlyAccess
   ```

2. **Verify Rekognition is available in your region:**
   ```bash
   aws rekognition list-collections --region eu-west-2
   ```

### 10. High Costs / Unexpected Charges

**Cause:** Rekognition charges per image analyzed.

**Solutions:**

1. **Monitor usage:**
   ```bash
   # Check Lambda invocations
   aws cloudwatch get-metric-statistics \
     --namespace AWS/Lambda \
     --metric-name Invocations \
     --dimensions Name=FunctionName,Value=ratemyrickshaw-lambda \
     --start-time 2024-01-01T00:00:00Z \
     --end-time 2024-12-31T23:59:59Z \
     --period 86400 \
     --statistics Sum \
     --region eu-west-2
   ```

2. **Set up billing alerts:**
   - AWS Console → Billing → Billing preferences
   - Enable "Receive Billing Alerts"
   - Create CloudWatch alarm for estimated charges

3. **Add rate limiting** (edit Lambda):
   - CloudWatch → Alarms
   - Set alarm for Lambda invocations > threshold
   - Or use API Gateway usage plans

## Debugging Tips

### Enable Debug Logging

1. **Lambda:**
   Edit `application.properties`:
   ```properties
   quarkus.log.level=DEBUG
   ```

2. **API Gateway:**
   - Console → API Gateway → Stages → prod
   - Logs/Tracing → Enable CloudWatch Logs
   - Log Level: INFO or ERROR

### Test Locally

1. **Run Lambda locally:**
   ```bash
   ./mvnw quarkus:dev
   
   # Test
   curl -X POST http://localhost:8080 \
     -H 'Content-Type: application/json' \
     -d '{"imageUrl":"https://example.com/image.jpg"}'
   ```

2. **Run frontend locally:**
   ```bash
   cd frontend
   REACT_APP_API_ENDPOINT=http://localhost:8080 npm start
   ```

### Clean Slate Deployment

If all else fails, clean up and redeploy:

```bash
# Delete everything
aws lambda delete-function --function-name ratemyrickshaw-lambda --region eu-west-2
API_ID=$(aws apigateway get-rest-apis --region eu-west-2 --query "items[?name=='ratemyrickshaw-api'].id" --output text)
aws apigateway delete-rest-api --rest-api-id $API_ID --region eu-west-2
aws s3 rm s3://ratemyrickshaw --recursive
aws s3 rb s3://ratemyrickshaw
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/AmazonRekognitionReadOnlyAccess
aws iam detach-role-policy --role-name ratemyrickshaw-lambda-role --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
aws iam delete-role --role-name ratemyrickshaw-lambda-role

# Redeploy
./deploy.sh
```

## Getting More Help

1. **Check AWS Service Health:**
   https://status.aws.amazon.com/

2. **AWS Documentation:**
   - Lambda: https://docs.aws.amazon.com/lambda/
   - Rekognition: https://docs.aws.amazon.com/rekognition/
   - API Gateway: https://docs.aws.amazon.com/apigateway/

3. **Quarkus Documentation:**
   https://quarkus.io/guides/amazon-lambda

4. **View detailed logs:**
   ```bash
   ./scripts/get-info.sh  # Shows deployment status
   aws logs tail /aws/lambda/ratemyrickshaw-lambda --follow
   ```
