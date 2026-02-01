(ns ring-http-exchange.model
  (:require
    [jj.sql.boa :as boa])
  (:import
    (java.util ArrayList)
    (java.util.concurrent ThreadLocalRandom)))
(def ^:private ^:const additional-message {:id      0
                                           :message "Additional fortune added at request time."})

(def ^:const hello-world "Hello, World!")
(def ^:const json-body {:message hello-world})

(def query-fortunes (boa/build-query (boa/->NextJdbcAdapter) "sql/fortune.sql"))
(def query-world-by-id (boa/build-query (boa/->NextJdbcAdapter) "sql/db.sql"))
(def update-world-by-id (boa/build-query (boa/->NextJdbcAdapter) "sql/updates.sql"))

(defn- random-number ^long []
  (unchecked-inc (.nextInt (ThreadLocalRandom/current) 10000)))

(defn parse-count ^long [query-params]
  (try
    (let [n (Integer/parseInt (get query-params "queries" "1"))]
      (Integer/max 1 (Integer/min n 500)))
    (catch Exception _ 1)))

(defn get-random-world [data-source]
  (first (query-world-by-id data-source {:id (random-number)})))

(defn get-random-worlds [data-source n]
  (let [worlds (ArrayList. ^int n)]
    (dotimes [_ n]
      (.add worlds (get-random-world data-source)))
    (vec worlds)))

(defn get-and-update-random-worlds [data-source n]
  (let [worlds (ArrayList. ^int n)]
    (dotimes [_ n]
      (let [world (get-random-world data-source)
            new-random-number (random-number)
            updated-world (assoc world :randomNumber new-random-number)]
        (update-world-by-id data-source {:id           (:id updated-world)
                                        :randomNumber new-random-number})
        (.add worlds updated-world)))
    (vec worlds)))

(defn get-fortunes-context [data-source]
  {:messages (as-> (query-fortunes data-source) fortunes
                   (conj fortunes additional-message)
                   (sort-by :message fortunes))}
  )