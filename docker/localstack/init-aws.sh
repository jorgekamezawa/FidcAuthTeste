#!/bin/bash

echo "Initializing LocalStack resources..."

# Create secrets
awslocal secretsmanager create-secret \
  --name dev/jwt/secret \
  --secret-string '{
    "signingKey": "local-secret-key-for-development-only-fidc-auth"
  }'

echo "LocalStack initialized successfully!"