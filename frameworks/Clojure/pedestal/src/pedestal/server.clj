(ns pedestal.server
  (:gen-class)
  (:require [io.pedestal.http :as server]
            [pedestal.service :as service]))

(defonce runnable-service
         (-> service/service
             server/default-interceptors
             server/create-server))


(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (server/start runnable-service))