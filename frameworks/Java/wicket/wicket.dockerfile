FROM maven:3-eclipse-temurin-21 as maven
WORKDIR /resin
RUN curl -sL http://caucho.com/download/resin-4.0.61.tar.gz | tar xz --strip-components=1

WORKDIR /project
COPY src src
COPY pom.xml pom.xml
RUN mvn compile war:war -q

FROM openjdk:25-jdk-slim
WORKDIR /resin
RUN rm -rf webapps/*
COPY --from=maven /resin /resin
COPY --from=maven /project/target/hellowicket-1.0.war webapps/ROOT.war
COPY resin.xml conf/resin.xml
EXPOSE 8080

CMD ["java", "-Xms2G", "-Xmx2G", "-server", "-XX:+UseParallelGC", "-jar", "lib/resin.jar", "console"]

