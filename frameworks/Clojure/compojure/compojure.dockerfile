FROM clojure:lein AS lein
WORKDIR /compojure
COPY project.clj project.clj
COPY src src
RUN lein ring uberwar

FROM  tomcat:9.0.111-jre25-temurin-noble
WORKDIR /usr/local/tomcat
COPY --from=lein /compojure/target/*.war webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]