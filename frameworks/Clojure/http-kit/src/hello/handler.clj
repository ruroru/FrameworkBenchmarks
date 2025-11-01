(ns hello.handler
  (:gen-class)
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.cli :as cli]
    [compojure.core :as compojure :refer [GET]]
    [compojure.route :as route]
    [hiccup.page :as hiccup-page]
    [hiccup.util :as hiccup-util]
    [hikari-cp.core :refer :all]
    [org.httpkit.server :as server]
    [ring.middleware.json :as json]
    [ring.util.response :as ring-resp]))

(defn sanitize-queries-param
  "Sanitizes the `queries` parameter. Clamps the value between 1 and 500.
  Invalid (string) values become 1."
  [queries]
  (let [n (try (Integer/parseInt queries)
               (catch Exception e 1))]
    (cond
      (< n 1) 1
      (> n 500) 500
      :else n)))

(def datasource-config
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  256
   :pool-name          "db-pool"
   :adapter            "mysql8"
   :username           "benchmarkdbuser"
   :password           "benchmarkdbpass"
   :database-name      "hello_world"
   :server-name        "tfb-database"
   :port-number        3306
   :register-mbeans    false})

(defonce datasource (atom nil))

(defn get-datasource []
  (when (nil? @datasource)
    (reset! datasource (make-datasource datasource-config)))
  @datasource)

(defn db-spec []
  {:datasource (get-datasource)})

(defn get-random-world
  "Query a random World record from the database"
  []
  (let [id (inc (rand-int 9999))]
    (first (jdbc/query (db-spec) ["select * from world where id = ?" id]))))

(defn run-queries
  "Run query repeatedly, return an array"
  [queries]
  (mapv (fn [_] (get-random-world))
        (range queries)))

(defn get-all-fortunes
  "Query all Fortune records from the database using JDBC."
  []
  (jdbc/query (db-spec) ["select * from fortune"]))

(defn get-fortunes
  "Fetch the full list of Fortunes from the database, sort them by the fortune
  message text, and then return the results."
  [query-function]
  (sort-by #(:message %)
           (conj
             (query-function)
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
         [:td (hiccup-util/escape-html (:message x))]])]]))

(defn update-and-persist
  [queries]
  (let [worlds (run-queries queries)]
    (mapv (fn [world]
            (let [new-random (inc (rand-int 9999))]
              (jdbc/execute! (db-spec)
                             ["UPDATE world SET randomnumber = ? WHERE id = ?" new-random (:id world)])
              (assoc world :randomnumber new-random)))
          worlds)))

(defn json-serialization
  "Test 1: JSON serialization"
  []
  (ring-resp/response {:message "Hello, World!"}))

(defn single-query-test
  "Test 2: Single database query - returns single record directly"
  []
  (ring-resp/response (get-random-world)))

(defn multiple-queries-test
  "Test 3: Multiple database queries (raw)"
  [queries]
  (-> queries
      (sanitize-queries-param)
      (run-queries)
      (ring-resp/response)))

(defn fortune-test
  "Test 4: Fortunes Raw"
  []
  (->
    (get-fortunes get-all-fortunes)
    (fortunes-hiccup)
    (ring-resp/response)
    (ring-resp/content-type "text/html")
    (ring-resp/charset "utf-8")))

(defn db-updates
  "Test 5: Database updates Raw"
  [queries]
  (-> queries
      (sanitize-queries-param)
      (update-and-persist)
      (ring-resp/response)))

(def plaintext
  "Test 6: Plaintext"
  (->
    (ring-resp/response "Hello, World!")
    (ring-resp/content-type "text/plain")))

(compojure/defroutes app-routes
                     (GET "/" [] "Hello, World!")
                     (GET "/plaintext" [] plaintext)
                     (GET "/json" [] (json-serialization))
                     (GET "/db" [] (single-query-test))
                     (GET "/queries/:queries" [queries] (multiple-queries-test queries))
                     (GET "/queries/" [] (multiple-queries-test "1"))
                     (GET "/fortunes" [] (fortune-test))
                     (GET "/updates/:queries" [queries] (db-updates queries))
                     (GET "/updates/" [] (db-updates "1"))
                     (route/not-found "Not Found"))

(defn parse-port [s]
  "Convert stringy port number int. Defaults to 8080."
  (cond
    (string? s) (Integer/parseInt s)
    (instance? Integer s) s
    (instance? Long s) (.intValue ^Long s)
    :else 8080))

(defn start-server [{:keys [port]}]
  (let [handler (json/wrap-json-response app-routes)
        cpu (.availableProcessors (Runtime/getRuntime))]
    (server/run-server handler {:port   port
                                :thread (* 2 cpu)})
    (println (str "http-kit server listens at :" port))))

(defn -main [& args]
  (let [[options _ banner]
        (cli/cli args
                 ["-p" "--port" "Port to listen" :default 8080 :parse-fn parse-port]
                 ["--[no-]help" "Print this help"])]
    (when (:help options)
      (println banner)
      (System/exit 0))
    (start-server options)))