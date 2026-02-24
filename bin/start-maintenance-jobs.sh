#!/bin/bash
set -e

echo "Starting Trend Service Maintenance Jobs..."

JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx256m}"

exec java ${JAVA_OPTS} \
    -jar /opt/trend-service/maintenance-jobs/target/trend-service-maintenance-jobs.jar \
    "$@"
