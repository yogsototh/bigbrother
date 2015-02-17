(ns bigbrother.counter
  (:require
   [clojure.algo.generic.functor :refer [fmap]]))

;; ------------------------------------------------------------------------------
;; COUNTERS

;; ## Monoid instance of `sumcounter`
;; a sum time is a list of couple `{name nb}`
(def empty-sumcounter {})
(defn add-sumcounter [st st2]
  (merge-with + st st2))

;; Counters atoms
(def counters (atom {}))
(def sumcounters (atom empty-sumcounter))

;; Counters (the mean is given by default)
(defn- set-counter! [k v] (swap! counters add-sumcounter {k v}))
(defn log-counter
  "declare a specific counters (not time)"
  ([k] (set-counter! k 1))
  ([k v] (set-counter! k v)))
(defn log-counter->
  "declare a specific counters (not time) and returns the first parameter"
  [x k v]
  (log-counter k v)
  x)

(defn loop-finished []
  ;; aggreate sumcounter
  (swap! sumcounters add-sumcounter @counters)
  (reset! counters {}))

(defn reset-acc! []
  (reset! sumcounters empty-sumcounter))

(defn resume [nb-ms]
  (fmap #(float (/ % (/ nb-ms 1000))) @sumcounters))
