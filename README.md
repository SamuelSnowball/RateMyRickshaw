
# Deployments #
Lambda only (code changes):
mvn clean package
sam.cmd deploy --stack-name ratemyrickshaw --region eu-west-2


Frontend only (UI changes):
cd frontend
npm run build
aws s3 sync build/ s3://ratemyrickshaw-prod-$(aws sts get-caller-identity --query Account --output text)/ --delete --region eu-west-2
aws cloudfront create-invalidation --distribution-id d97ukxjft5frq --paths "/*"
cd ..


DNS update (run ONCE or when CloudFront changes):
./update-dns.sh