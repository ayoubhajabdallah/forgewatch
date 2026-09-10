FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Windows checkouts may have CRLF line endings and no executable bit.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
# Integration tests need PostgreSQL; run them separately from image packaging.
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-noble AS runtime
RUN groupadd --gid 10001 forgewatch \
    && useradd --uid 10001 --gid forgewatch --no-create-home --shell /usr/sbin/nologin forgewatch
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
