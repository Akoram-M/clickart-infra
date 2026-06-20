#!/bin/bash

# ==========================================
# PATH RESOLUTION
# ==========================================
SERVICES_DIR=$(pwd)
UI_DIR="$(dirname "$SERVICES_DIR")/clickart-ui"

# ==========================================
# MASTER PIPELINE FLAGS
# ==========================================
BUILD=true
DEPLOY=true
IS_ALL_SERVICES=true

# ==========================================
# INDIVIDUAL SERVICE FLAGS
# ==========================================
RUN_CONFIG_SERVER=true
RUN_DISCOVERY_SERVER=true
RUN_API_GATEWAY=true

RUN_INVENTORY_SERVICE=true
RUN_ORDER_SERVICE=true
RUN_NOTIFICATION_SERVICE=true
RUN_PRODUCT_SERVICE=true

RUN_CLICKART_UI=true

PIPELINE_FAILED=false
DEPLOY_COMMANDS=()

# ==========================================
# HELPER LOGIC
# ==========================================
check_run() {
    local SERVICE_FLAG=$1
    if [ "$IS_ALL_SERVICES" = true ]; then return 0; fi
    if [ "$SERVICE_FLAG" = true ]; then return 0; fi
    return 1
}

# ==========================================
# SYNCHRONOUS BUILD LOGIC
# ==========================================
handle_build() {
    local DIR_NAME=$1
    local FLAG=$2
    local FULL_PATH="$SERVICES_DIR/$DIR_NAME"

    if ! check_run "$FLAG"; then return 0; fi

    echo "⚙️  BUILDING: $DIR_NAME"
    if [ ! -d "$FULL_PATH" ] || [ ! -f "$FULL_PATH/pom.xml" ]; then
        echo "❌ FATAL: Cannot find pom.xml at $FULL_PATH"
        PIPELINE_FAILED=true
        return 1
    fi

    mvn -f "$FULL_PATH/pom.xml" clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ FATAL: Build failed for $DIR_NAME."
        PIPELINE_FAILED=true
        return 1
    fi
}

handle_ui_build() {
    if ! check_run "$RUN_CLICKART_UI"; then return 0; fi

    echo "⚙️  PREPARING UI: clickart-ui"
    if [ ! -d "$UI_DIR" ] || [ ! -f "$UI_DIR/package.json" ]; then
        echo "❌ FATAL: Cannot find package.json at $UI_DIR"
        PIPELINE_FAILED=true
        return 1
    fi

    (cd "$UI_DIR" && npm install)
    if [ $? -ne 0 ]; then
        echo "❌ FATAL: NPM Install failed for UI."
        PIPELINE_FAILED=true
        return 1
    fi
}

# ==========================================
# GENERATE DEPLOY COMMANDS
# ==========================================
generate_deploy_command() {
    local DIR_NAME=$1
    local FLAG=$2
    local FULL_PATH="$SERVICES_DIR/$DIR_NAME"

    if check_run "$FLAG"; then
        DEPLOY_COMMANDS+=("cd '$FULL_PATH' && mvn spring-boot:run")
    fi
}

generate_ui_deploy_command() {
    if check_run "$RUN_CLICKART_UI"; then
        DEPLOY_COMMANDS+=("cd '$UI_DIR' && npm run dev")
    fi
}

# ==========================================
# EXECUTION PHASE
# ==========================================
if [ "$BUILD" = true ]; then
    echo "================================================"
    echo "   STARTING SYNCHRONOUS BUILD PHASE"
    echo "================================================"
    handle_build "config-server" "$RUN_CONFIG_SERVER" || true
    [ "$PIPELINE_FAILED" = false ] && handle_build "discovery-server" "$RUN_DISCOVERY_SERVER" || true
    [ "$PIPELINE_FAILED" = false ] && handle_build "api-gateway" "$RUN_API_GATEWAY" || true
    [ "$PIPELINE_FAILED" = false ] && handle_build "inventory-service" "$RUN_INVENTORY_SERVICE" || true
    [ "$PIPELINE_FAILED" = false ] && handle_build "order-service" "$RUN_ORDER_SERVICE" || true
    [ "$PIPELINE_FAILED" = false ] && handle_build "notification-service" "$RUN_NOTIFICATION_SERVICE" || true
    [ "$PIPELINE_FAILED" = false ] && handle_build "product-service" "$RUN_PRODUCT_SERVICE" || true
    [ "$PIPELINE_FAILED" = false ] && handle_ui_build || true
fi

if [ "$PIPELINE_FAILED" = true ]; then
    echo "================================================"
    echo "❌ PIPELINE TERMINATED DUE TO BUILD ERRORS."
    echo "Fix the compilation errors above before deploying."
    echo "================================================"
    exit 1
fi

if [ "$DEPLOY" = true ]; then
    generate_deploy_command "config-server" "$RUN_CONFIG_SERVER"
    generate_deploy_command "discovery-server" "$RUN_DISCOVERY_SERVER"
    generate_deploy_command "api-gateway" "$RUN_API_GATEWAY"
    generate_deploy_command "inventory-service" "$RUN_INVENTORY_SERVICE"
    generate_deploy_command "order-service" "$RUN_ORDER_SERVICE"
    generate_deploy_command "notification-service" "$RUN_NOTIFICATION_SERVICE"
    generate_deploy_command "product-service" "$RUN_PRODUCT_SERVICE"
    generate_ui_deploy_command
    
    echo ""
    echo "================================================"
    echo "✅ BUILDS SUCCESSFUL. RUNBOOK GENERATED."
    echo "================================================"
    echo "To clear previously running instances and free ports, run this first:"
    echo "killall -9 java 2>/dev/null; killall -9 node 2>/dev/null"
    echo "------------------------------------------------"
    echo "Open your terminal tabs and paste the following commands:"
    echo ""
    
    for cmd in "${DEPLOY_COMMANDS[@]}"; do
        echo "$cmd"
        echo ""
    done
fi