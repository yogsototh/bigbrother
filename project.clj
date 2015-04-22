(defproject bigbrother "0.1.4.2"
  :description "Periodically send metrics"
  :url "http://github.com/yogsototh/bigbrother"
  :license {:name "MIT"
            :url "http://chosealicense.com/licenses/mit"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/test.check "0.7.0"]
                 [riemann-clojure-client  "0.3.2"]
                 [overtone/at-at "1.2.0"]])
