FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline \
    -Dmaven.test.skip=true \
    --no-transfer-progress \
    -Dstyle.color=never

COPY src ./src

# Las pruebas y reglas ArchUnit forman parte obligatoria del build de la imagen.
# Si Maven falla, se imprime todo el log antes de detener la imagen.
RUN mvn clean package \
      --no-transfer-progress \
      -Dstyle.color=never \
      > /tmp/maven-build.log 2>&1 \
    && grep -E "Tests run:|BUILD SUCCESS" /tmp/maven-build.log \
    || (cat /tmp/maven-build.log && exit 1)

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/andina-seguros-clean-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
