#!/bin/bash

echo "Initializing LocalStack resources..."

# Set AWS region for LocalStack
export AWS_DEFAULT_REGION=us-east-1

# Create secrets with valid JWT signing key (HS256 - 32 bytes base64 encoded)
aws --endpoint-url=http://localhost:4566 \
  --region us-east-1 \
  secretsmanager create-secret \
  --name dev/jwt/secret \
  --secret-string '{"signingKey": "dGhpc2lzYXNlY3JldGtleWZvcmp3dHNpZ25pbmdmaWRjYXV0aGRldg=="}'

# Create database configuration secret
aws --endpoint-url=http://localhost:4566 \
  --region us-east-1 \
  secretsmanager create-secret \
  --name dev/db/config \
  --secret-string '{"username": "fidc_auth_user", "password": "senha123", "host": "localhost", "port": 5432, "dbname": "fidc_auth_db"}'

echo "LocalStack initialized successfully!"