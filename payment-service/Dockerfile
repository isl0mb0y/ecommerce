# Build from the repository root: docker build -f payment-service/Dockerfile -t payment-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY payment-service/src payment-service/src
RUN mvn -q -pl payment-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
