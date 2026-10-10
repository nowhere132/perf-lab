# Base images are pinned (tag and digest) so a JDK or Maven update can never
# change what the graded thresholds were calibrated against.
FROM maven:3.9.16-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -o package -DskipTests
RUN java -Djarmode=tools -jar target/order-router.jar extract --layers --destination /layers

# A full JDK, not a JRE: jcmd, jstack, jfr and jstat are part of the toolbox.
FROM eclipse-temurin:25.0.4.1_1-jdk@sha256:119a3d18f160a3e7655a66034d0f43beee31cd7b3b9142d57a5de29772011de6
ARG ASPROF=4.5
RUN apt-get update -qq && apt-get install -y -qq --no-install-recommends curl procps >/dev/null \
 && curl -fsSL https://github.com/async-profiler/async-profiler/releases/download/v${ASPROF}/async-profiler-${ASPROF}-linux-x64.tar.gz \
    | tar xz -C /opt && ln -s /opt/async-profiler-${ASPROF}-linux-x64 /opt/async-profiler \
 && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/async-profiler/bin:${PATH}"
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/order-router.jar"]
