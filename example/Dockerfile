# -buster is required to have apt available
FROM openjdk:17-jdk-slim

# by default curl is not available, so install it
RUN apt update && apt install curl -y

WORKDIR /app

# Copy AAS model
# COPY ./resources/model.aasx /app/model.aasx
# Copy FA³ST config
# COPY ./resources/demoConfig.json /app/demoConfig.json
# Copy config
# COPY ./configurations /configurations
# COPY ./resources /resources
# Copy jar
COPY ./build/libs/dataspace-connector.jar /app

ENV EDC_FS_CONFIG=$EDC_FS_CONFIG

ENTRYPOINT ["java", "-jar", "dataspace-connector.jar"]
