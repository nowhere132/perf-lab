FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -o package -DskipTests

# A full JDK, not a JRE: jcmd, jstack, jfr and jstat are part of the toolbox.
FROM eclipse-temurin:25-jdk
ARG ASPROF=4.5
RUN apt-get update -qq && apt-get install -y -qq --no-install-recommends curl procps >/dev/null \
 && curl -fsSL https://github.com/async-profiler/async-profiler/releases/download/v${ASPROF}/async-profiler-${ASPROF}-linux-x64.tar.gz \
    | tar xz -C /opt && ln -s /opt/async-profiler-${ASPROF}-linux-x64 /opt/async-profiler \
 && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/async-profiler/bin:${PATH}"
WORKDIR /app
COPY --from=build /src/target/order-router.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
