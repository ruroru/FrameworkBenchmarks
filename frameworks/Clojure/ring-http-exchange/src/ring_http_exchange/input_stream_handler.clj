(ns ring-http-exchange.input-stream-handler
  (:require
    [jj.majavat :as majavat]
    [jj.majavat.renderer :refer [->InputStreamRenderer]]
    [ring-http-exchange.model :as model])
  (:import (java.io ByteArrayInputStream)
           (java.util.function Consumer)))

(defrecord Response [body status headers])

(def ^:private hello-world-bytes (.getBytes "Hello, World!"))
(def ^:private ^:const fortune-headers {"Server"       "ring-http-exchange"
                                        "Content-Type" "text/html; charset=UTF-8"})


(def ^:private render-fortune (majavat/build-html-renderer "fortune.html"
                                                           {:renderer (->InputStreamRenderer)}))

(defn get-handler [data-source]
  (fn [req]
    (case (req :uri)
      "/fortunes" (Response. (render-fortune (model/fortunes-body data-source)) 200 fortune-headers)
      (Response. (ByteArrayInputStream. hello-world-bytes) 200 {"Server"       "ring-http-exchange"
                                                                "Content-Type" "text/plain"}))))

(deftype FortuneCallback [respond]
  Consumer
  (accept [_ fortune-data]
    (respond
      (Response.

        (render-fortune {:messages (as->
                                     (conj fortune-data {:id      0
                                                         :message "Additional fortune added at request time."}) fortunes
                                     (sort-by :message fortunes))})
        200
        fortune-headers))))

(defn get-async-handler [data-source]
  (fn [req respond raise]
    (case (req :uri)
      "/fortunes" (model/async-query-fortunes data-source (FortuneCallback. respond) raise)
      (Response. (ByteArrayInputStream. hello-world-bytes) 200 {"Server"       "ring-http-exchange"
                                                                "Content-Type" "text/plain"}))))

