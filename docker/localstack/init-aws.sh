#!/bin/bash

echo "=== Initializing LocalStack resources ==="

# Set AWS region for LocalStack
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
until curl -s http://localhost:4566/_localstack/health | grep -q "running"; do
  echo "Waiting for LocalStack..."
  sleep 2
done
echo "LocalStack is ready!"

# Create secrets with valid JWT signing key (HS256 - 32 bytes base64 encoded)
echo "Creating JWT secret..."
aws --endpoint-url=http://localhost:4566 \
  --region us-east-1 \
  secretsmanager create-secret \
  --name dev/jwt/secret \
  --secret-string '{"signingKey": "dGhpc2lzYXNlY3JldGtleWZvcmp3dHNpZ25pbmdmaWRjYXV0aGRldg=="}' \
  --description "JWT signing key for FIDC Auth development"

if [ $? -eq 0 ]; then
  echo "✅ JWT secret created successfully"
else
  echo "❌ Failed to create JWT secret"
fi

# Create database configuration secret  
echo "Creating database configuration secret..."
aws --endpoint-url=http://localhost:4566 \
  --region us-east-1 \
  secretsmanager create-secret \
  --name dev/db/config \
  --secret-string '{"username": "fidc_auth_user", "password": "senha123", "host": "fidc-auth-postgres", "port": 5432, "dbname": "fidc_auth_db"}' \
  --description "Database configuration for FIDC Auth development"

if [ $? -eq 0 ]; then
  echo "✅ Database config secret created successfully"
else
  echo "❌ Failed to create database config secret"
fi

# List created secrets for verification
echo "=== Listing created secrets ==="
aws --endpoint-url=http://localhost:4566 \
  --region us-east-1 \
  secretsmanager list-secrets

echo "=== LocalStack initialization completed ==="