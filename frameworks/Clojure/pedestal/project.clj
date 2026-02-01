(defproject pedestal "0.1"
  :description "A Clojure-Pedestal server for testing in the Framework Benchmarks"
  :url "https://github.com/TechEmpower/FrameworkBenchmarks"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [io.pedestal/pedestal.service "0.6.5"]
                 [io.pedestal/pedestal.jetty "0.6.5"]
                 [com.github.seancorfield/next.jdbc "1.3.1093"]
                 [hikari-cp "4.0.0"]
                 [hiccup "1.0.5"]
                 [org.clojars.jj/majavat "1.19.0"]]
  :managed-dependencies [[org.slf4j/slf4j-api "2.0.17"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config"]
  :profiles {
             :srepl {:jvm-opts ^:replace ["-d64" "-server"
                                          "-XX:+UseG1GC"
                                          "-D\"clojure.compiler.direct-linking=true\""
                                          "-Dclojure.server.repl={:port 5555 :accept clojure.core.server/repl}"]}
             :dev {:aliases {"crepl" ["trampoline" "run" "-m" "clojure.main/main"]
                             "srepl" ["with-profile" "srepl" "trampoline" "run" "-m" "clojure.main/main"]
                             "run-dev" ["trampoline" "run" "-m" "pedestal.server/run-dev"]}
                   :dependencies [
                                  [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]
                                  [org.slf4j/jul-to-slf4j "1.7.22"]
                                  [org.slf4j/jcl-over-slf4j "1.7.22"]
                                  [org.slf4j/log4j-over-slf4j "1.7.22"]
                                  [criterium "0.4.4"]]}}
  :auto-clean false

  :pedantic? :abort
  :main pedestal.server
  :aot [pedestal.server]
  :uberjar-name "pedestal-standalone.jar")