(defproject hello "reitit"
  :description "pohjavirta, reitit, jsonista & porsas"
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [metosin/pohjavirta "0.0.1-alpha5"]
                 [metosin/porsas "0.0.1-alpha13"]
                 [metosin/jsonista "0.3.13"]
                 [metosin/reitit "0.9.1"]
                 [hikari-cp "3.3.0"]]
  :main hello.handler
  :plugins [[lein-ancient "1.0.0-RC3"]]
  :aot :all)
