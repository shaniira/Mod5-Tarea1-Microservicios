FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests --no-transfer-progress -Dstyle.color=never
COPY src ./src
RUN mvn clean package -DskipTests --no-transfer-progress -Dstyle.color=never

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/andina-notification-consumer-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
