(ns ring-http-exchange.input-stream-handler
  (:require
    [jj.majavat :as majavat]
    [jj.majavat.renderer :refer [->InputStreamRenderer]]
    [jsonista.core :as json]
    [ring-http-exchange.model :as model])
  (:import (java.io ByteArrayInputStream)
           (java.net URLDecoder)))

(defrecord Response [body status headers])

(def ^:private ^:const fortune-headers {"Server"       "ring-http-exchange"
                                        "Content-Type" "text/html; charset=UTF-8"})
(def ^:private ^:const json-headers {"Server"       "ring-http-exchange"
                                     "Content-Type" "application/json"})
(def ^:private ^:const plain-text-headers {"Server"       "ring-http-exchange"
                                           "Content-Type" "text/plain"})

(def ^:private render-fortune (majavat/build-html-renderer "fortune.html"
                                                           {:renderer (->InputStreamRenderer)}))

(defn- json-bytes [data]
  (ByteArrayInputStream. (.getBytes (json/write-value-as-string data))))

(defn- parse-query-string
  "Parse query string manually since Ring middleware might not be configured"
  [query-string]
  (when query-string
    (into {}
          (for [param (clojure.string/split ^String query-string #"&")]
            (let [[k v] (clojure.string/split ^String param #"=" 2)]
              [(URLDecoder/decode ^String k "UTF-8")
               (when v (URLDecoder/decode ^String v "UTF-8"))])))))

(defn get-handler [data-source]
  (fn [req]
    (case (req :uri)
      "/fortunes" (Response. (render-fortune (model/get-fortunes-context data-source)) 200 fortune-headers)
      "/db" (Response. (json-bytes (model/get-random-world data-source)) 200 json-headers)
      "/queries" (let [query-params (or (req :query-params)
                                        (parse-query-string (req :query-string)))
                       count (model/parse-count query-params)
                       worlds (model/get-random-worlds data-source count)]
                   (Response. (json-bytes worlds) 200 json-headers))
      "/updates" (let [query-params (or (req :query-params)
                                        (parse-query-string (req :query-string)))
                       count (model/parse-count query-params)
                       worlds (model/get-and-update-random-worlds data-source count)]
                   (Response. (json-bytes worlds) 200 json-headers))
      (Response. (ByteArrayInputStream. (.getBytes model/hello-world)) 200 plain-text-headers))))