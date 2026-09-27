# --- Etapa 1: build (las pruebas y las reglas ArchUnit son obligatorias) ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -Dstyle.color=never
COPY src ./src
RUN mvn -B clean package --no-transfer-progress -Dstyle.color=never \
      > /tmp/maven-build.log 2>&1 \
    && grep -E "Tests run:|BUILD SUCCESS" /tmp/maven-build.log \
    || (cat /tmp/maven-build.log && exit 1)

# --- Etapa 2: runtime (sin JDK ni Maven, usuario sin privilegios) ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /build/target/customer-service-*.jar app.jar
RUN chown -R spring:spring /app
USER spring
# Solo lo alcanza el gateway; no se publica en el host.
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness > /dev/null || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
