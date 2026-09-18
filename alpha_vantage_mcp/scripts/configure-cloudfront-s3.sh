#!/bin/bash

# Script to configure CloudFront to S3 Static Website Hosting access
AWS_PROFILE=${AWS_PROFILE:-default}

# Load environment variables from .env if it exists
if [ -f ".env" ]; then
    echo "🔧 Loading environment variables from .env..."
    export $(grep -v '^#' .env | xargs)
fi

set -e

echo "🔧 Configuring S3 Static Website Hosting..."

STATIC_FILES_BUCKET=${STATIC_FILES_BUCKET:-alphavantage-mcp-web}
echo "🪣 Using S3 bucket: $STATIC_FILES_BUCKET"

# Get AWS region  
AWS_REGION=$(aws configure get region --profile $AWS_PROFILE)
echo "🌍 AWS Region: $AWS_REGION"

# Step 1: Unblock public access (required for static website hosting)
echo "🔓 Allowing public read access for static website hosting..."
aws s3api put-public-access-block \
    --profile $AWS_PROFILE \
    --bucket $STATIC_FILES_BUCKET \
    --public-access-block-configuration \
    'BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false'

# Step 2: Enable S3 Static Website Hosting
echo "🌐 Enabling S3 Static Website Hosting..."
aws s3api put-bucket-website \
    --profile $AWS_PROFILE \
    --bucket $STATIC_FILES_BUCKET \
    --website-configuration \
    'IndexDocument={Suffix=index.html},ErrorDocument={Key=index.html}'

# Step 3: Create public read policy for static website hosting
echo "📄 Creating S3 bucket policy for public read access..."
cat > /tmp/bucket-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${STATIC_FILES_BUCKET}/*"
    }
  ]
}
EOF

echo "🔒 Applying S3 bucket policy..."
aws s3api put-bucket-policy \
    --profile $AWS_PROFILE \
    --bucket $STATIC_FILES_BUCKET \
    --policy file:///tmp/bucket-policy.json

# Clean up temporary file
rm /tmp/bucket-policy.json

echo "✅ S3 Static Website Hosting configuration complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Deploy your CloudFormation stack with the updated template"
echo "   2. Test static file access through CloudFront"
echo "   3. Upload files to the /artifacts/ directory"
echo ""
echo "🌐 S3 Website Endpoint: http://${STATIC_FILES_BUCKET}.s3-website-${AWS_REGION}.amazonaws.com"
echo "🪣 S3 Bucket: $STATIC_FILES_BUCKET"