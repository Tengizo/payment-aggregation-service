FROM openjdk:17-oracle

WORKDIR /app

COPY target/payment-aggregation-service-*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
