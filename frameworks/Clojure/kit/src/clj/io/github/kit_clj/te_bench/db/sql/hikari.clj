(ns io.github.kit-clj.te-bench.db.sql.hikari
  (:require
    [clojure.pprint :as pprint]
    [integrant.core :as ig]
    [kit.edge.db.sql.hikari]))

(defmethod ig/prep-key :db.sql/hikari-connection
  [_ config]
  (pprint/pprint config)
  (assoc config :maximum-pool-size 520))
