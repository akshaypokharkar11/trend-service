#!/bin/bash
set -e

echo "Starting Trend Service Web API..."

JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

exec java ${JAVA_OPTS} \
    -jar /opt/trend-service/web/target/trend-service-web.jar \
    "$@"
