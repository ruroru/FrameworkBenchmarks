(ns ring-http-exchange.string-handler
  (:require
    [jj.majavat :as majavat]
    [jj.tassu :refer [async-route route GET]]
    [ring-http-exchange.model :as model]))

(defrecord Response [body status headers])
(def ^:private ^:const fortune-headers {"Server"       "ring-http-exchange"
                                        "Content-Type" "text/html; charset=UTF-8"})
(def ^:private ^:const json-headers {"Server"       "ring-http-exchange"
                                     "Content-Type" "application/json"})
(def ^:private ^:const plain-text-headers {"Server"       "ring-http-exchange"
                                           "Content-Type" "text/plain"})

(def ^:private render-fortune (majavat/build-html-renderer "fortune.html"))

(defn create-callback
  ([respond]
   (fn [fortune-data]
     (respond
       (Response.
         (render-fortune {:messages
                          (sort-by :message
                                   (conj fortune-data {:id      0
                                                       :message "Additional fortune added at request time."}))})
         200
         fortune-headers))))
  ([respond ^java.util.concurrent.Executor executor]
   (fn [fortune-data]
     (.execute executor
       (fn []
         (respond
           (Response.
             (render-fortune {:messages
                              (sort-by :message
                                       (conj fortune-data {:id      0
                                                           :message "Additional fortune added at request time."}))})
             200
             fortune-headers)))))))

(defn get-handler [data-source]
  (route {"/plaintext" [(GET (fn [_req]
                               (Response. model/hello-world 200 plain-text-headers)))]
          "/json"      [(GET (fn [_req]
                               (Response. (model/json-body) 200 json-headers)))]
          "/fortunes"  [(GET (fn [_req]
                               (Response. (render-fortune (model/fortunes-body data-source)) 200 fortune-headers)))]
          "/"          [(GET (fn [_req]
                               (Response. model/hello-world 200 {"Server"       "ring-http-exchange"
                                                                  "Content-Type" "text/plain"})))]}))

(defn get-async-handler [data-source executor]
  (async-route {"/fortunes" [(GET (fn [req respond raise]
                                    (model/async-query-fortunes data-source (create-callback respond executor) raise)))]
                "/"         [(GET (fn [_req respond _raise]
                                    (respond (Response. model/hello-world 200 plain-text-headers))))]}))

(defn get-vertx-handler [data-source]
  (async-route {"/fortunes" [(GET (fn [req respond raise]
                                    (model/vertx-query-fortunes data-source (create-callback respond) raise)))]
                "/"         [(GET (fn [_req respond _raise]
                                    (respond (Response. model/hello-world 200 plain-text-headers))))]}))