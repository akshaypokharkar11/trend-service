#!/bin/sh
# Maven Wrapper startup script
# Simplified version - downloads Maven if not present

set -e

MAVEN_VERSION="3.9.9"
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

# Check if Maven wrapper jar exists
WRAPPER_JAR="$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
    # Use the Maven wrapper jar
    exec java \
        -classpath "$WRAPPER_JAR" \
        org.apache.maven.wrapper.MavenWrapperMain "$@"
fi

# Fallback: try system Maven
if command -v mvn > /dev/null 2>&1; then
    exec mvn "$@"
fi

# Fallback: download Maven directly
if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven ${MAVEN_VERSION}..."
    mkdir -p "$(dirname "$MAVEN_HOME")"
    curl -sL "$MAVEN_URL" | tar xz -C "$(dirname "$MAVEN_HOME")"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
