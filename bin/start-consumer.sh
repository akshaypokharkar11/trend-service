#!/bin/bash
set -e

echo "Starting Trend Service Kafka Consumer..."

JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

exec java ${JAVA_OPTS} \
    -jar /opt/trend-service/kafka/target/trend-service-consumer.jar \
    "$@"
