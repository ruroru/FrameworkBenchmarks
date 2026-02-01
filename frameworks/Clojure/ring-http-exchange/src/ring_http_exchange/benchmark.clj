(ns ring-http-exchange.benchmark
  (:gen-class)
  (:require
    [next.jdbc.connection :as connection]
    [ring-http-exchange.input-stream-handler :as input-stream-handler]
    [ring-http-exchange.core :as server]
    [ring-http-exchange.string-handler :as string-handler])
  (:import
    (com.zaxxer.hikari HikariDataSource)
    (java.util.concurrent Executors)))

(def db-spec {:idle-timeout      15000
              :max-lifetime      60000
              :minimum-idle      0
              :maximum-pool-size 1024
              :jdbcUrl           "jdbc:postgresql://tfb-database/hello_world?user=benchmarkdbuser&password=benchmarkdbpass&tlsnowait=true"})

(defn -main
  [& args]
  (println "Starting server on port 8080")
  (let [data-source (connection/->pool HikariDataSource db-spec)
        use-inputstream? (some #{"--inputstream" "-i" "inputstream"} args)]
    (.addDataSourceProperty data-source "tcpKeepAlive" "true")
    (.addDataSourceProperty data-source "useSSL" false)
    (.addDataSourceProperty data-source "prepStmtCacheSize" "250")
    (.addDataSourceProperty data-source "cachePrepStmts" "true")
    (.addDataSourceProperty data-source "prepStmtCacheSqlLimit" "2048")
    (server/run-http-server
      (if use-inputstream?
        (input-stream-handler/get-handler data-source)
        (string-handler/get-handler data-source))
      {:port     8080
       :host     "0.0.0.0"
       :executor (Executors/newCachedThreadPool)})))