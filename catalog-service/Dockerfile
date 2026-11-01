# Build from the repository root: docker build -f catalog-service/Dockerfile -t catalog-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY catalog-service/src catalog-service/src
RUN mvn -q -pl catalog-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/catalog-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
