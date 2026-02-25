#!/bin/bash
# Update Route53 DNS record to point to CloudFront distribution

set -e

# Configuration
STACK_NAME="ratemyrickshaw"
REGION="eu-west-2"
DOMAIN_NAME="ratemyrickshaw.snowballsjourney.com"
HOSTED_ZONE_NAME="snowballsjourney.com"

echo "========================================="
echo "Route53 DNS Update"
echo "========================================="
echo "Domain: $DOMAIN_NAME"
echo "Hosted Zone: $HOSTED_ZONE_NAME"
echo ""

# Get CloudFront distribution URL from stack outputs
echo "Fetching CloudFront distribution URL..."
CLOUDFRONT_URL=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`CloudFrontURL`].OutputValue' \
    --output text 2>/dev/null || echo "")

if [ -z "$CLOUDFRONT_URL" ]; then
    echo "❌ Error: Could not retrieve CloudFront URL from stack outputs"
    echo "   Make sure CloudFront is deployed"
    exit 1
fi

echo "CloudFront URL: $CLOUDFRONT_URL"
echo ""

# Get the hosted zone ID
echo "Finding Route53 hosted zone..."
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
    --query "HostedZones[?Name=='${HOSTED_ZONE_NAME}.'].Id" \
    --output text | cut -d'/' -f3)

if [ -z "$HOSTED_ZONE_ID" ]; then
    echo "❌ Error: Could not find hosted zone for $HOSTED_ZONE_NAME"
    echo "   Make sure the domain is hosted in Route53"
    exit 1
fi

echo "Hosted Zone ID: $HOSTED_ZONE_ID"
echo ""

# Create the change batch JSON
cat > route53-change.json <<EOF
{
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "$DOMAIN_NAME",
        "Type": "CNAME",
        "TTL": 300,
        "ResourceRecords": [
          {
            "Value": "$CLOUDFRONT_URL"
          }
        ]
      }
    }
  ]
}
EOF

# Update the DNS record
echo "Updating DNS record..."
CHANGE_ID=$(aws route53 change-resource-record-sets \
    --hosted-zone-id $HOSTED_ZONE_ID \
    --change-batch file://route53-change.json \
    --query 'ChangeInfo.Id' \
    --output text)

if [ -z "$CHANGE_ID" ]; then
    echo "❌ Error: Failed to update DNS record"
    exit 1
fi

echo "✅ DNS record updated successfully"
echo "Change ID: $CHANGE_ID"
echo ""

# Wait for change to propagate
echo "Waiting for DNS change to propagate..."
aws route53 wait resource-record-sets-changed --id "$CHANGE_ID"

echo ""
echo "========================================="
echo "✅ DNS Update Complete!"
echo "========================================="
echo ""
echo "Domain: $DOMAIN_NAME"
echo "Points to: $CLOUDFRONT_URL"
echo ""
echo "The DNS change has been applied to Route53."
echo "It may take 5-15 minutes for changes to propagate globally."
echo ""
echo "Test with:"
echo "  nslookup $DOMAIN_NAME 8.8.8.8"
echo ""
echo "Visit your site at:"
echo "  https://$DOMAIN_NAME"
echo ""

# Cleanup
rm -f route53-change.json
