(defproject hello "compojure"
  :description "FrameworkBenchmarks test implementations"
  :url "http://localhost:3000/"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [compojure "1.7.2"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-servlet "1.15.3"]
                 [ring/ring-core "1.15.3"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 [com.mysql/mysql-connector-j "9.5.0"]
                 [com.github.seancorfield/next.jdbc "1.3.1070"]
                 [hikari-cp "3.3.0"]
                 [hiccup "1.0.5"]]
  :repositories {"Sonatype releases" "https://oss.sonatype.org/content/repositories/releases/"}
  :plugins [[lein-ring "0.12.5"]
            [lein-ancient "1.0.0-RC3"]
            [org.clojure/core.unify "0.5.7"]
            [nightlight/lein-nightlight "RELEASE"]]
  :ring {:handler      hello.handler/app
         :servlet-name "compojure-bench"}
  :profiles {:dev {:dependencies [[ring/ring-mock "0.6.2"]]}})
