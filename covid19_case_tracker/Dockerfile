# syntax=docker/dockerfile:1

# ---- Build -------------------------------------------------------------------
FROM eclipse-temurin:24-jdk AS build
WORKDIR /workspace

# Resolve dependencies in their own layer so source changes don't re-download them.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests \
    && java -Djarmode=tools -jar target/covid19-tracker.jar extract --layers --destination target/extracted

# ---- Runtime -----------------------------------------------------------------
FROM eclipse-temurin:24-jre
WORKDIR /app

RUN useradd --system --no-create-home --shell /usr/sbin/nologin tracker

# Least- to most-frequently changing, to make the most of the layer cache.
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER tracker
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "covid19-tracker.jar"]
