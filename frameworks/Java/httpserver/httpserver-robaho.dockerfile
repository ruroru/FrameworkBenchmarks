# Multi-stage build using FreeBSD
FROM freebsd/freebsd-static:latest as maven

# Install OpenJDK and Maven
RUN pkg update && pkg install -y openjdk23 maven

ENV JAVA_HOME=/usr/local/openjdk23

WORKDIR /httpserver-robaho
COPY pom.xml pom.xml
COPY src src

RUN mvn compile -P robaho assembly:single -q

# Final stage
FROM freebsd/freebsd-static:latest

# Install OpenJDK runtime
RUN pkg update && pkg install -y openjdk23

# Set JAVA_HOME
ENV JAVA_HOME=/usr/local/openjdk23

WORKDIR /httpserver-robaho
COPY --from=maven /httpserver-robaho/target/httpserver-1.0-jar-with-dependencies.jar app.jar

EXPOSE 8080

CMD ["java", "-server", "-jar", "app.jar"]
