#!/bin/bash

echo "Registering internal services..."

AUTH_SERVICE_URL="http://localhost:8082/api/v1"
ADMIN_TOKEN="your-admin-token-here"

# Register User Service
curl -X POST "$AUTH_SERVICE_URL/service/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "serviceName=user-service" \
  -d "description=User Management Service" \
  -d "allowedScopes[]=service" \
  -d "allowedScopes[]=user:read" \
  -d "allowedScopes[]=user:write"

# Register Chat Service
curl -X POST "$AUTH_SERVICE_URL/service/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "serviceName=chat-service" \
  -d "description=Real-time Chat Service" \
  -d "allowedScopes[]=service" \
  -d "allowedScopes[]=message:read" \
  -d "allowedScopes[]=message:write"

# Register Payment Service
curl -X POST "$AUTH_SERVICE_URL/service/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "serviceName=payment-service" \
  -d "description=Payment Processing Service" \
  -d "allowedScopes[]=service" \
  -d "allowedScopes[]=payment:process" \
  -d "allowedScopes[]=payment:read"

# Register Wallet Service
curl -X POST "$AUTH_SERVICE_URL/service/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "serviceName=wallet-service" \
  -d "description=Digital Wallet Service" \
  -d "allowedScopes[]=service" \
  -d "allowedScopes[]=wallet:read" \
  -d "allowedScopes[]=wallet:write"

echo "Service registration complete!"