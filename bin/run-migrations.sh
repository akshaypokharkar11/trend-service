#!/bin/bash
set -e

echo "Running database migrations..."

JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx256m}"

exec java ${JAVA_OPTS} \
    -jar /opt/trend-service/dbmigrator/target/trend-service-dbmigrator.jar \
    "$@"
