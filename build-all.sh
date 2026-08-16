#!/bin/bash

# Define strict exit on failure
set -e

# Array of custom Spring Boot services
SERVICES=(
  "config-server"
  "discovery-server"
  "product-service"
  "inventory-service"
  "order-service"
  "notification-service"
  "api-gateway"
)

echo "Starting Unified Build & Containerization Pipeline..."

cd /Users/akorammohandoss/workspace/backend/Clickart/clickart-services

for SERVICE in "${SERVICES[@]}"; do
  echo "=========================================="
  echo "Processing $SERVICE"
  echo "=========================================="
  
  cd "$SERVICE"
  
  # 1. Compile Java binary
  echo "-> Running Maven Clean Package..."
  mvn clean package -DskipTests
  
  # 2. Burn new Docker image (bypassing cache to ensure latest code)
  echo "-> Building Docker Image..."
  docker build --no-cache -t clickart/"$SERVICE":1.0 .
  
  cd ..
done

echo "=========================================="
echo "Pipeline Complete. All custom images are updated."
echo "Execute 'docker compose up -d' in your root directory to deploy the new images."
echo "Your databases and Keycloak volumes will remain untouched."
echo "=========================================="