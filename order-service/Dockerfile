# Build from the repository root: docker build -f order-service/Dockerfile -t order-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY order-service/src order-service/src
RUN mvn -q -pl order-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
