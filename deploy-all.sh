#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "========================================"
echo " Starting Clickart K8s Deployment"
echo "========================================"

echo "[1/4] Compiling Java Services..."
cd clickart-services
mvn clean package -DskipTests
cd ..

echo "[2/4] Building Docker Images..."
cd clickart-services
docker build -t clickart/config-server:1.0 ./config-server
docker build -t clickart/discovery-server:1.0 ./discovery-server
docker build -t clickart/api-gateway:1.0 ./api-gateway
docker build -t clickart/product-service:1.0 ./product-service
docker build -t clickart/inventory-service:1.0 ./inventory-service
docker build -t clickart/order-service:1.0 ./order-service
docker build -t clickart/notification-service:1.0 ./notification-service
cd ..

echo "[3/4] Provisioning Kubernetes Infrastructure..."
kubectl config use-context docker-desktop

# Create namespace, ignore error if it already exists
kubectl create namespace clickart --dry-run=client -o yaml | kubectl apply -f -

# Apply infrastructure
kubectl apply -f k8s/infra/ -n clickart

echo "Waiting for infrastructure to initialize (This may take a minute)..."
# Wait for Postgres and Kafka to be ready before starting Spring Boot
kubectl wait --for=condition=ready pod -l app=postgres -n clickart --timeout=120s
kubectl wait --for=condition=ready pod -l app=kafka -n clickart --timeout=120s
kubectl wait --for=condition=ready pod -l app=keycloak -n clickart --timeout=120s

echo "[4/4] Deploying Microservices..."
kubectl apply -f k8s/services/discovery-server.yaml -n clickart
kubectl apply -f k8s/services/config-server.yaml -n clickart

echo "Giving Eureka 20 seconds to boot up..."
sleep 20

kubectl apply -f k8s/services/api-gateway.yaml -n clickart
kubectl apply -f k8s/services/product-service.yaml -n clickart
kubectl apply -f k8s/services/order-service.yaml -n clickart
kubectl apply -f k8s/services/inventory-service.yaml -n clickart
kubectl apply -f k8s/services/notification-service.yaml -n clickart

# Apply Ingress routing
kubectl apply -f k8s/ingress.yaml

echo "========================================"
echo " Deployment Triggered Successfully."
echo " Run 'kubectl get pods -n clickart -w' to monitor startup."
echo "========================================"