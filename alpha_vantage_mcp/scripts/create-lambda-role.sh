#!/bin/bash

# Load environment variables from .env if it exists
if [ -f ".env" ]; then
    echo "🔧 Loading environment variables from .env..."
    export $(grep -v '^#' .env | xargs)
fi

set -e

ROLE_NAME="mcp-server-lambda-execution-role"
POLICY_ARN="arn:aws:iam::aws:policy/AWSLambdaExecute"
VPC_POLICY_ARN="arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"

# Set AWS profile options if specified
AWS_PROFILE_OPTION=""
if [ ! -z "$AWS_PROFILE" ]; then
    AWS_PROFILE_OPTION="--profile $AWS_PROFILE"
    echo "Using AWS profile: $AWS_PROFILE"
fi

echo "Managing Lambda execution role: $ROLE_NAME"
echo ""

# Check for existing role
echo "Checking for existing role..."
EXISTING_ROLE=$(aws iam get-role $AWS_PROFILE_OPTION --role-name "$ROLE_NAME" --query "Role.Arn" --output text 2>/dev/null || echo "")

if [ ! -z "$EXISTING_ROLE" ] && [ "$EXISTING_ROLE" != "None" ]; then
    echo "Found existing role:"
    echo "Role ARN: $EXISTING_ROLE"
    
    # Check if policies are attached
    echo "Checking policy attachments..."
    BASIC_POLICY_ATTACHED=$(aws iam list-attached-role-policies $AWS_PROFILE_OPTION --role-name "$ROLE_NAME" --query "AttachedPolicies[?PolicyArn=='$POLICY_ARN'].PolicyArn" --output text 2>/dev/null || echo "")
    VPC_POLICY_ATTACHED=$(aws iam list-attached-role-policies $AWS_PROFILE_OPTION --role-name "$ROLE_NAME" --query "AttachedPolicies[?PolicyArn=='$VPC_POLICY_ARN'].PolicyArn" --output text 2>/dev/null || echo "")
    
    if [ ! -z "$BASIC_POLICY_ATTACHED" ] && [ ! -z "$VPC_POLICY_ATTACHED" ]; then
        echo "✅ Role already exists with both policies attached!"
        echo "Role ARN: $EXISTING_ROLE"
        exit 0
    else
        echo "Role exists but some policies not attached. Attaching missing policies..."
        if [ -z "$BASIC_POLICY_ATTACHED" ]; then
            aws iam attach-role-policy $AWS_PROFILE_OPTION --role-name "$ROLE_NAME" --policy-arn "$POLICY_ARN"
            echo "✅ AWSLambdaExecute policy attached!"
        fi
        if [ -z "$VPC_POLICY_ATTACHED" ]; then
            aws iam attach-role-policy $AWS_PROFILE_OPTION --role-name "$ROLE_NAME" --policy-arn "$VPC_POLICY_ARN"
            echo "✅ AWSLambdaVPCAccessExecutionRole policy attached!"
        fi
        echo "Role ARN: $EXISTING_ROLE"
        exit 0
    fi
fi

echo "No existing role found. Creating new role..."

# Trust policy document
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

# Create role (ignore if it already exists)
if ROLE_ARN=$(aws iam create-role $AWS_PROFILE_OPTION \
    --role-name "$ROLE_NAME" \
    --assume-role-policy-document "$TRUST_POLICY" \
    --query "Role.Arn" \
    --output text 2>/dev/null); then
    echo "Role created successfully!"
    echo "Role ARN: $ROLE_ARN"
else
    echo "Role already exists, continuing with policy attachment..."
    # Construct the expected ARN format
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity $AWS_PROFILE_OPTION --query "Account" --output text)
    ROLE_ARN="arn:aws:iam::${AWS_ACCOUNT_ID}:role/${ROLE_NAME}"
    echo "Using role ARN: $ROLE_ARN"
fi
echo ""

# Attach policies
echo "Attaching AWSLambdaExecute policy..."
aws iam attach-role-policy $AWS_PROFILE_OPTION \
    --role-name "$ROLE_NAME" \
    --policy-arn "$POLICY_ARN"

echo "Attaching AWSLambdaVPCAccessExecutionRole policy..."
aws iam attach-role-policy $AWS_PROFILE_OPTION \
    --role-name "$ROLE_NAME" \
    --policy-arn "$VPC_POLICY_ARN"

echo "✅ Lambda execution role setup complete!"
echo "Role ARN: $ROLE_ARN"
echo ""
echo "This role provides:"
echo "- Basic Lambda execution permissions"
echo "- CloudWatch Logs access"
echo "- S3 read/write access"
echo "- VPC network interface creation and management"
echo ""
echo "Role is ready to use in your Lambda functions and CloudFormation templates."