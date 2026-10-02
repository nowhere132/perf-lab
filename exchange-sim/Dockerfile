FROM eclipse-temurin:25-jdk
WORKDIR /sim
COPY ExchangeSim.java .
RUN javac ExchangeSim.java
EXPOSE 9000
CMD ["java", "-Xmx256m", "-cp", "/sim", "ExchangeSim"]
