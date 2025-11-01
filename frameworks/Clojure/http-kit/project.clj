(defproject hello "http-kit"
  :description "FrameworkBenchmarks test implementations"
  :url "http://localhost:8080/"
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [compojure "1.7.2"]
                 [ring/ring-json "0.5.1"]
                 [org.clojure/tools.cli "1.2.245"]
                 [http-kit "2.8.1"]
                 [javax.xml.bind/jaxb-api "2.3.1"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 [com.mysql/mysql-connector-j "9.5.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [hikari-cp "3.3.0"]
                 [hiccup "1.0.5"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-ancient "1.0.0-RC3"]]
  :ring {:handler hello.handler/app}
  :main hello.handler
  :aot [hello.handler]
  :uberjar-name "http-kit-standalone.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.6.2"]]}})
