# Sample API Requests and Expected Responses

## Request Format

### Analyze from URL
```json
{
  "imageUrl": "https://example.com/rickshaw.jpg"
}
```

### Analyze from S3
```json
{
  "s3Bucket": "my-bucket",
  "s3Key": "images/rickshaw.jpg"
}
```

## Response Format

### Successful Rickshaw Detection
```json
{
  "success": true,
  "message": "Image analyzed successfully",
  "isRickshaw": true,
  "rickshawConfidence": 95.5,
  "labels": [
    "Vehicle",
    "Transportation",
    "Auto Rickshaw",
    "Rickshaw",
    "Three Wheeler",
    "Taxi",
    "Car",
    "Wheel",
    "Machine"
  ],
  "labelConfidence": {
    "Vehicle": 99.8,
    "Transportation": 98.5,
    "Auto Rickshaw": 95.5,
    "Rickshaw": 94.2,
    "Three Wheeler": 92.1,
    "Taxi": 89.3,
    "Car": 87.6,
    "Wheel": 96.7,
    "Machine": 91.2
  }
}
```

### Non-Rickshaw Image
```json
{
  "success": true,
  "message": "Image analyzed successfully",
  "isRickshaw": false,
  "rickshawConfidence": 0.0,
  "labels": [
    "Animal",
    "Cat",
    "Mammal",
    "Pet",
    "Kitten"
  ],
  "labelConfidence": {
    "Animal": 99.9,
    "Cat": 99.8,
    "Mammal": 99.7,
    "Pet": 98.5,
    "Kitten": 95.3
  }
}
```

### Error Response (Invalid Request)
```json
{
  "success": false,
  "message": "Either s3Bucket/s3Key or imageUrl must be provided",
  "isRickshaw": false,
  "rickshawConfidence": 0.0,
  "labels": null,
  "labelConfidence": null
}
```

### Error Response (Rekognition Error)
```json
{
  "success": false,
  "message": "Rekognition error: Invalid image format",
  "isRickshaw": false,
  "rickshawConfidence": 0.0,
  "labels": null,
  "labelConfidence": null
}
```

## Sample cURL Commands

### Test with Rickshaw Image
```bash
curl -X POST https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Auto_rickshaw_in_Thrissur.jpg/640px-Auto_rickshaw_in_Thrissur.jpg"
  }'
```

### Test with Non-Rickshaw Image
```bash
curl -X POST https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/640px-Cat03.jpg"
  }'
```

### Test with S3 Image
```bash
curl -X POST https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "s3Bucket": "ratemyrickshaw",
    "s3Key": "test-images/rickshaw1.jpg"
  }'
```

### Test with Invalid Request
```bash
curl -X POST https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod/analyze \
  -H 'Content-Type: application/json' \
  -d '{}'
```

## Test Image URLs

### Rickshaw Images (Should Detect)
```
https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Auto_rickshaw_in_Thrissur.jpg/640px-Auto_rickshaw_in_Thrissur.jpg

https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Auto_rickshaw_Mumbai.jpg/640px-Auto_rickshaw_Mumbai.jpg

https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/New_Delhi_Tuk-Tuk.jpg/640px-New_Delhi_Tuk-Tuk.jpg
```

### Non-Rickshaw Images (Should Not Detect)
```
https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/640px-Cat03.jpg

https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Eiffel_Tower_from_Champ_de_Mars.jpg/640px-Eiffel_Tower_from_Champ_de_Mars.jpg

https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/New_york_times_square-terabass.jpg/640px-New_york_times_square-terabass.jpg
```

## JavaScript Fetch Example

### React/Frontend Usage
```javascript
const analyzeImage = async (imageUrl) => {
  const response = await fetch(
    'https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod/analyze',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ imageUrl })
    }
  );
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  
  return await response.json();
};

// Usage
analyzeImage('https://example.com/rickshaw.jpg')
  .then(result => {
    if (result.success && result.isRickshaw) {
      console.log(`Rickshaw detected with ${result.rickshawConfidence}% confidence`);
      console.log('Labels:', result.labels);
    } else {
      console.log('No rickshaw detected');
    }
  })
  .catch(error => console.error('Error:', error));
```

### Node.js Example
```javascript
const https = require('https');

const analyzeImage = (imageUrl) => {
  const data = JSON.stringify({ imageUrl });
  
  const options = {
    hostname: 'YOUR_API_ID.execute-api.eu-west-2.amazonaws.com',
    path: '/prod/analyze',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': data.length
    }
  };
  
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => resolve(JSON.parse(body)));
    });
    
    req.on('error', reject);
    req.write(data);
    req.end();
  });
};

// Usage
analyzeImage('https://example.com/rickshaw.jpg')
  .then(console.log)
  .catch(console.error);
```

### Python Example
```python
import requests
import json

def analyze_image(image_url):
    endpoint = 'https://YOUR_API_ID.execute-api.eu-west-2.amazonaws.com/prod/analyze'
    
    payload = {
        'imageUrl': image_url
    }
    
    response = requests.post(
        endpoint,
        json=payload,
        headers={'Content-Type': 'application/json'}
    )
    
    response.raise_for_status()
    return response.json()

# Usage
result = analyze_image('https://example.com/rickshaw.jpg')

if result['success'] and result['isRickshaw']:
    print(f"Rickshaw detected! Confidence: {result['rickshawConfidence']}%")
    print(f"Labels: {', '.join(result['labels'])}")
else:
    print("No rickshaw detected")
```

## Performance Metrics

### Expected Response Times

| Scenario | Cold Start | Warm Start |
|----------|-----------|------------|
| URL Analysis | 2-4 seconds | 1-2 seconds |
| S3 Analysis | 1-3 seconds | 0.5-1.5 seconds |

### Response Size
- Typical response: ~500-1000 bytes
- With 20 labels: ~800-1500 bytes

## HTTP Status Codes

| Code | Meaning | When it Occurs |
|------|---------|----------------|
| 200 | Success | Request processed successfully (check `success` field in response) |
| 400 | Bad Request | Invalid JSON or missing required fields |
| 403 | Forbidden | API key invalid (if API key authentication is enabled) |
| 500 | Internal Server Error | Lambda function error or Rekognition error |
| 502 | Bad Gateway | Lambda timeout or configuration error |
| 504 | Gateway Timeout | Request took too long (>30 seconds) |

## Troubleshooting

### Issue: 502 Bad Gateway
**Likely Cause:** Lambda function error

**Debug:**
```bash
aws logs tail /aws/lambda/ratemyrickshaw-lambda --follow --region eu-west-2
```

### Issue: Slow Response Times
**Likely Cause:** Cold start

**Solution:** 
- Increase Lambda memory (more CPU)
- Use provisioned concurrency
- Implement keep-warm pings

### Issue: "Image format not supported"
**Likely Cause:** Invalid image URL or format

**Supported Formats:**
- JPEG
- PNG
- Maximum size: 15 MB (URL) or 5 MB (S3)

## Rate Limits

### AWS Rekognition Limits
- 50 transactions per second (TPS) per account
- Burst capacity: up to 100 TPS

### API Gateway Limits
- Default: 10,000 requests per second
- Burst: 5,000 requests

### Recommended Client-Side Throttling
- Max 10 requests per second
- Implement exponential backoff for retries
- Cache results when possible
