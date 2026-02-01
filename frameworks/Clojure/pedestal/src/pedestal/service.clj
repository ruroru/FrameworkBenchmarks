(ns pedestal.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [hikari-cp.core :refer :all]
            [hiccup.page :refer [html5]]
            [hiccup.util :refer [escape-html]]))

(defn make-hikari-data-source
  []
  (make-datasource {
                    :idle-timeout      15000
                    :max-lifetime      60000
                    :minimum-idle      0
                    :maximum-pool-size 1024
                    :pool-name         "db-pool"
                    :driver-class-name "com.mysql.cj.jdbc.Driver"
                    :jdbc-url          "jdbc:mysql://tfb-database:3306/hello_world?jdbcCompliantTruncation=false&elideSetAutoCommits=true&useLocalSessionState=true&cachePrepStmts=true&cacheCallableStmts=true&alwaysSendSetIsolation=false&prepStmtCacheSize=4096&cacheServerConfiguration=true&prepStmtCacheSqlLimit=2048&zeroDateTimeBehavior=convertToNull&traceProtocol=false&useUnbufferedInput=false&useReadAheadInput=false&maintainTimeStats=false&useServerPrepStmts=true&cacheRSMetadata=true&useSSL=false"
                    :username          "benchmarkdbuser"
                    :password          "benchmarkdbpass"
                    :register-mbeans   false}))

(def memoize-hikari-data-source (memoize make-hikari-data-source))

(defn db-mysql [] (memoize-hikari-data-source))

(defn json-serialization
  "Test 1: JSON serialization"
  [request]
  (http/json-response {:message "Hello, World!"}))

(defn get-random-world
  "Query a random World record from the database"
  []
  (let [id (inc (rand-int 9999))]
    (jdbc/execute! (db-mysql)
                   ["select * from world where id = ?" id]
                   {:builder-fn rs/as-unqualified-lower-maps})))

(defn sanitize-queries-param
  "Sanitizes the `queries` parameter. Clamps the value between 1 and 500.
  Invalid (string) values become 1."
  [request]
  (let [queries (get-in request [:query-params :queries] "1")
        n (try (Integer/parseInt queries)
               (catch Exception _ 1))]
    (cond
      (< n 1) 1
      (> n 500) 500
      :else n)))

(defn run-queries
  "Run query repeatedly, return an array"
  [count]
  (flatten
    (take count
          (repeatedly get-random-world))))

(defn get-all-fortunes
  "Query all Fortune records from the database using next.jdbc."
  []
  (jdbc/execute! (db-mysql)
                 ["select * from fortune"]
                 {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-fortunes
  "Fetch the full list of Fortunes from the database, sort them by the fortune
  message text, and then return the results."
  []
  (sort-by #(:message %)
           (conj
             (get-all-fortunes)
             {:id 0 :message "Additional fortune added at request time."})))

(defn fortunes-hiccup
  "Render the given fortunes to simple HTML using Hiccup."
  [fortunes]
  (html5
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
         [:td (escape-html (:message x))]])]]))

(defn update-and-persist
  "Using next.jdbc: Changes the :randomNumber of a number of world entities.
  Persists the changes to sql then returns the updated entities"
  [queries]
  (let [world (map #(assoc % :randomnumber (inc (rand-int 9999))) (run-queries queries))]
    (doseq [{:keys [id randomnumber]} world]
      (jdbc/execute-one!
        (db-mysql)
        ["UPDATE world SET randomnumber = ? WHERE id = ?" randomnumber id]))
    world))

(defn single-query-test
  "Test 2: Single database query"
  [request]
  (http/json-response (first (run-queries 1))))

(defn multiple-queries-test
  "Test 3: Multiple database queries"
  [request]
  (-> request
      (sanitize-queries-param)
      (run-queries)
      (http/json-response)))

(defn fortune-test
  "Test 4: Fortunes"
  [request]
  (->
    (get-fortunes)
    (fortunes-hiccup)
    (ring-resp/response)
    (ring-resp/content-type "text/html")
    (ring-resp/charset "utf-8")))

(defn db-updates
  "Test 5: Database updates"
  [request]
  (-> request
      (sanitize-queries-param)
      (update-and-persist)
      (http/json-response)))


(defn plaintext
  "Test 6: Plaintext"
  [_]
  (-> (ring-resp/response "Hello, World!")
      (ring-resp/content-type "text/plain")))

;; Define routes using table syntax
(def routes
  (route/expand-routes
    #{["/plaintext" :get plaintext :route-name :plaintext]
      ["/json" :get json-serialization :route-name :json]
      ["/db" :get single-query-test :route-name :db]
      ["/queries" :get multiple-queries-test :route-name :queries]
      ["/fortunes" :get fortune-test :route-name :fortunes]
      ["/updates" :get db-updates :route-name :updates]}))

(def service
  "How the server will look, not the code to start it up"
  {:env                 :dev
   ::http/routes        routes
   ::http/resource-path "/public"
   ::http/type          :jetty
   ::http/host          "0.0.0.0"
   ::http/port          8080})