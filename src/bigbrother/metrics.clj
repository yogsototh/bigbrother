(ns bigbrother.metrics
  (:require [clojure.algo.generic.functor :refer [fmap]]))

;; ------------------------------------------------------------------------------
;; Sum Metrics

;; ## Monoid instance of `summetrics`
;; a sum time is a list of couple `{name metric-value}`
(def empty-summetric {})
(defn add-summetrics [st st2]
  (merge-with + st st2))

(def metrics (atom {})) ;; timestamps
(def summetrics (atom empty-summetric)) ;; time spent by type

;; Metrics (the mean is given by default)
(defn- set-metric! [k v] (swap! metrics add-summetrics {k v}))
(defn log-metric
  "declare a specific metrics (not time)"
  [k v]
  (set-metric! k v))
(defn log-metric->
  "declare a specific metrics (not time) and returns the first parameter"
  [x k v]
  (log-metric k v)
  x)

(defn loop-finished []
  ;; aggreate summetrics
  (swap! summetrics add-summetrics @metrics)
  (reset! metrics {}))

(defn reset-acc! []
  (reset! summetrics empty-summetric))

(defn resume [n]
  (fmap #(float (/ % n)) @summetrics))
