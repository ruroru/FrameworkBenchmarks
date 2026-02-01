FROM clojure:lein as lein
WORKDIR /pedestal
COPY config config
COPY src src
COPY test test
COPY project.clj project.clj
RUN lein uberjar

FROM amazoncorretto:25
WORKDIR /pedestal
COPY --from=lein /pedestal/target/pedestal-standalone.jar .
EXPOSE 8080
CMD ["java", "-XX:MaxRAMPercentage=70", "-Dclojure.compiler.direct-linking=true", "-Dio.pedestal.log.defaultMetricsRecorder=nil", "-Dio.pedestal.log.overrideLogger=nil", "-jar", "pedestal-standalone.jar"]