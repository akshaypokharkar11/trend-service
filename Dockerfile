FROM eclipse-temurin:21-jdk

WORKDIR /opt/trend-service/

# Copy startup scripts
COPY bin/ bin/

# Copy application JARs
COPY web/target/trend-service-web.jar web/target/trend-service-web.jar
COPY kafka/target/trend-service-consumer.jar kafka/target/trend-service-consumer.jar
COPY dbmigrator/target/trend-service-dbmigrator.jar dbmigrator/target/trend-service-dbmigrator.jar
COPY maintenance-jobs/target/trend-service-maintenance-jobs.jar maintenance-jobs/target/trend-service-maintenance-jobs.jar

# Default: start the web service
CMD ["/opt/trend-service/bin/start-web.sh"]
