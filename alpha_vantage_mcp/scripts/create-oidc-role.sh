#!/bin/bash

# Load environment variables from .env if it exists
if [ -f ".env" ]; then
    echo "🔧 Loading environment variables from .env..."
    export $(grep -v '^#' .env | xargs)
fi

set -e

# Configuration
GITHUB_ORG_REPO="$1"  # e.g., "myorg/myrepo"
AWS_REGION="${2:-us-east-1}"
ROLE_NAME="${3:-GitHubActionsRole}"

if [ -z "$GITHUB_ORG_REPO" ]; then
    echo "Usage: $0 <github-org/repo> [aws-region] [role-name]"
    echo "Example: $0 myorg/myrepo us-east-1 GitHubActionsRole"
    exit 1
fi

echo "Creating OIDC Identity Provider and IAM Role for GitHub Actions..."
echo "GitHub Repo: $GITHUB_ORG_REPO"
echo "AWS Region: $AWS_REGION"
echo "Role Name: $ROLE_NAME"

# Get AWS Account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "AWS Account ID: $ACCOUNT_ID"

# Create OIDC Identity Provider
echo "Creating OIDC Identity Provider..."
aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 || echo "OIDC provider may already exist"

# Create trust policy
TRUST_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": [
            "repo:${GITHUB_ORG_REPO}:ref:refs/heads/main",
            "repo:${GITHUB_ORG_REPO}:ref:refs/heads/test"
          ]
        }
      }
    }
  ]
}
EOF
)

# Create IAM role
echo "Creating IAM Role: $ROLE_NAME..."
aws iam create-role \
    --role-name "$ROLE_NAME" \
    --assume-role-policy-document "$TRUST_POLICY" \
    --description "Role for GitHub Actions OIDC deployment" || echo "Role may already exist"

# Create deployment policy
DEPLOYMENT_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": [
        "arn:aws:s3:::*",
        "arn:aws:s3:::*/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation",
        "cloudfront:GetInvalidation",
        "cloudfront:ListInvalidations"
      ],
      "Resource": "*"
    }
  ]
}
EOF
)

# Create and attach policy
POLICY_NAME="${ROLE_NAME}DeploymentPolicy"
echo "Creating deployment policy: $POLICY_NAME..."
aws iam create-policy \
    --policy-name "$POLICY_NAME" \
    --policy-document "$DEPLOYMENT_POLICY" \
    --description "Deployment permissions for GitHub Actions" 2>/dev/null || echo "Policy may already exist"

echo "Attaching policy to role..."
aws iam attach-role-policy \
    --role-name "$ROLE_NAME" \
    --policy-arn "arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"

echo ""
echo "✅ Setup complete!"
echo ""
echo "Add this to your GitHub repository secrets:"
echo "AWS_ROLE_ARN_PROD: arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
echo "AWS_REGION_PROD: ${AWS_REGION}"
echo ""
echo "Role ARN: arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"