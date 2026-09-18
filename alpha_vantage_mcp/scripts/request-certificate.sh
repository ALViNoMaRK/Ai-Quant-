#!/bin/bash

# Load environment variables from .env if it exists
if [ -f ".env" ]; then
    echo "🔧 Loading environment variables from .env..."
    export $(grep -v '^#' .env | xargs)
fi

set -e

REGION="us-east-1"

if [ -z "$DOMAIN_NAME" ]; then
    echo "❌ Error: DOMAIN_NAME not set in .env file"
    exit 1
fi

# Set AWS profile options if specified
AWS_PROFILE_OPTION=""
if [ ! -z "$AWS_PROFILE" ]; then
    AWS_PROFILE_OPTION="--profile $AWS_PROFILE"
    echo "Using AWS profile: $AWS_PROFILE"
fi

echo "Managing SSL certificate for domain: $DOMAIN_NAME"
echo "Region: $REGION"
echo ""

# Check for existing certificates
echo "Checking for existing certificates..."
EXISTING_CERT=$(aws acm list-certificates $AWS_PROFILE_OPTION --region $REGION --query "CertificateSummaryList[?DomainName=='$DOMAIN_NAME'].CertificateArn" --output text)

if [ ! -z "$EXISTING_CERT" ] && [ "$EXISTING_CERT" != "None" ]; then
    echo "Found existing certificate:"
    echo "Certificate ARN: $EXISTING_CERT"
    
    # Check certificate status
    CERT_STATUS=$(aws acm describe-certificate $AWS_PROFILE_OPTION --certificate-arn "$EXISTING_CERT" --region $REGION --query "Certificate.Status" --output text)
    echo "Certificate Status: $CERT_STATUS"
    
    if [ "$CERT_STATUS" = "ISSUED" ]; then
        echo ""
        echo "✅ Certificate is already issued and ready to use!"
        echo "Add this ARN to your template.yaml CertificateArn parameter:"
        echo "$EXISTING_CERT"
        
        exit 0
    else
        echo "Certificate exists but is not issued (Status: $CERT_STATUS)"
        echo "Certificate ARN: $EXISTING_CERT"
        exit 0
    fi
fi

echo "No existing certificate found. Requesting new certificate..."

# Request new certificate
CERT_ARN=$(aws acm request-certificate $AWS_PROFILE_OPTION \
    --domain-name "$DOMAIN_NAME" \
    --validation-method DNS \
    --region $REGION \
    --query "CertificateArn" \
    --output text)

echo "Certificate requested successfully!"
echo "Certificate ARN: $CERT_ARN"
echo ""

# Wait a moment for AWS to process the request
echo "Waiting for DNS validation records to be available..."
sleep 5

# Get DNS validation records
echo "DNS Validation Records (copy these to your DNS provider):"
echo "========================================================="

aws acm describe-certificate $AWS_PROFILE_OPTION \
    --certificate-arn "$CERT_ARN" \
    --region $REGION \
    --query "Certificate.DomainValidationOptions[0].ResourceRecord" \
    --output table

echo ""

echo "Instructions:"
echo "1. Copy the CNAME record above to your DNS provider"
echo "2. Wait for DNS propagation (can take up to 30 minutes)"
echo "3. AWS will automatically validate and issue the certificate"
echo "4. Use this ARN in your template.yaml: $CERT_ARN"
echo ""
echo "To check certificate status later, run:"
if [ ! -z "$AWS_PROFILE" ]; then
    echo "aws acm describe-certificate --profile $AWS_PROFILE --certificate-arn $CERT_ARN --region $REGION --query Certificate.Status --output text"
else
    echo "aws acm describe-certificate --certificate-arn $CERT_ARN --region $REGION --query Certificate.Status --output text"
fi