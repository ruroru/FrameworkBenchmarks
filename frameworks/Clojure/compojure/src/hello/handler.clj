(ns hello.handler
  (:require
    [compojure.core :as compojure :refer [GET]]
    [compojure.route :as route]
    [hiccup.page :as hiccup-page]
    [hiccup.util :as hiccup-util]
    [next.jdbc :as jdbc]
    [next.jdbc.connection :as connection]
    [next.jdbc.result-set :as rs]
    [ring.middleware.json :as ring-json]
    [ring.util.response :as ring-resp])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def jdbc-opts {:builder-fn rs/as-unqualified-lower-maps})

(defn sanitize-queries-param
  "Sanitizes the `queries` parameter. Clamps the value between 1 and 500.
  Invalid (string) values become 1."
  [queries]
  (let [n (try (Integer/parseInt queries)
               (catch Exception e 1))]                      ; default to 1 on parse failure
    (cond
      (< n 1) 1
      (> n 500) 500
      :else n)))


(def db-spec
  {:jdbcUrl "jdbc:mysql://tfb-database:3306/hello_world?jdbcCompliantTruncation=false&elideSetAutoCommits=true&useLocalSessionState=true&cachePrepStmts=true&cacheCallableStmts=true&alwaysSendSetIsolation=false&prepStmtCacheSize=4096&cacheServerConfiguration=true&prepStmtCacheSqlLimit=2048&zeroDateTimeBehavior=convertToNull&traceProtocol=false&useUnbufferedInput=false&useReadAheadInput=false&maintainTimeStats=false&useServerPrepStmts=true&cacheRSMetadata=true&useSSL=false"})

(defn- get-random-world-raw
  "Query a random World record from the database"
  [data-source]
  (let [id (inc (rand-int 9999))]
    (jdbc/execute! data-source ["SELECT * FROM world WHERE id = ?" id] jdbc-opts)))

(defn- run-queries-raw
  "Run query repeatedly, return an array"
  [data-source queries]
  (flatten                                                  ; Make it a list of maps
    (take queries
          (repeatedly #(get-random-world-raw data-source)))))

(defn- get-all-fortunes-raw
  "Query all Fortune records from the database using JDBC."
  [data-source]
  (jdbc/execute! data-source ["SELECT * FROM fortune"] jdbc-opts))

(defn- get-fortunes
  "Fetch the full list of Fortunes from the database, sort them by the fortune
  message text, and then return the results."
  [data-source query-function]
  (sort-by #(:message %)
           (conj
             (query-function data-source)
             {:id 0 :message "Additional fortune added at request time."})))

(defn fortunes-hiccup
  "Render the given fortunes to simple HTML using Hiccup."
  [fortunes]
  (hiccup-page/html5
    [:head
     [:title "Fortunes"]]
    [:body
     [:table
      [:tr
       [:th "id"]
       [:th "message"]]
      (for [x fortunes]
        [:tr
         [:td (:id x)]
         [:td (hiccup-util/escape-html (:message x))]])
      ]]))


(defn update-and-persist-raw
  "Using JDBC: Changes the :randomNumber of a number of world entities.
  Persists the changes to sql then returns the updated entities"
  [data-source queries]
  (let [worlds (run-queries-raw data-source queries)
        updated-worlds (map #(assoc % :randomnumber (inc (rand-int 9999))) worlds)]
    (doseq [{:keys [id randomnumber]} updated-worlds]
      (jdbc/execute-one! data-source
                         ["UPDATE world SET randomNumber = ? WHERE id = ?" randomnumber id]
                         jdbc-opts))
    updated-worlds))

(defn json-serialization
  "Test 1: JSON serialization"
  []
  (-> (ring-resp/response {:message "Hello, World!"})
      (ring-resp/header "server" "compojure")))

(defn single-query-test-raw
  "Test 2: Single database query (raw)"
  [data-source]
  (-> (run-queries-raw data-source 1)
      (first)
      (ring-resp/response)
      (ring-resp/header "server" "compojure")))

(defn multiple-queries-test-raw
  "Test 3: Multiple database queries (raw)"
  [data-source queries]
  (-> queries
      (sanitize-queries-param)
      (run-queries-raw data-source)
      (ring-resp/response)
      (ring-resp/header "server" "compojure")))


(defn fortune-test-raw
  "Test 4: Fortunes Raw"
  [data-source]
  (->
    (get-fortunes data-source get-all-fortunes-raw)
    (fortunes-hiccup)
    (ring-resp/response)
    (ring-resp/content-type "text/html")
    (ring-resp/charset "utf-8")
    (ring-resp/header "server" "compojure")))

(defn db-updates-raw
  "Test 5: Database updates Raw"
  [data-source queries]
  (-> queries
      (sanitize-queries-param)
      (update-and-persist-raw data-source)
      (ring-resp/response)
      (ring-resp/header "server" "compojure")))

(def plaintext
  "Test 6: Plaintext"
  (->
    (ring-resp/response "Hello, World!")
    (ring-resp/content-type "text/plain")
    (ring-resp/header "server" "compojure")))

(defn app-routes [data-source]
  (compojure/routes
    (GET "/" [] "Hello, World!")
    (GET "/plaintext" [] plaintext)
    (GET "/json" [] (json-serialization))
    (GET "/db" [] (single-query-test-raw data-source))
    (GET "/queries/:queries" [queries] (multiple-queries-test-raw data-source queries))
    (GET "/queries/" [] (multiple-queries-test-raw data-source "1"))
    (GET "/fortunes" [] (fortune-test-raw data-source))
    (GET "/updates/:queries" [queries] (db-updates-raw data-source queries))
    (GET "/updates/" [] (db-updates-raw data-source "1"))
    (GET "/raw/db" [] (single-query-test-raw data-source))
    (GET "/raw/queries/:queries" [queries] (multiple-queries-test-raw data-source queries))
    (GET "/raw/queries/" [] (multiple-queries-test-raw data-source "1"))
    (GET "/raw/updates/:queries" [queries] (db-updates-raw data-source queries))
    (GET "/raw/updates/" [] (db-updates-raw data-source "1"))
    (route/not-found "Not Found")))

(def app
  "Format responses as JSON"
  (let [data-source (try
                      (connection/->pool HikariDataSource db-spec)
                      (catch Exception e
                        (.printStackTrace ^Exception e)))]
    (-> (app-routes data-source)
        (ring-json/wrap-json-response))))