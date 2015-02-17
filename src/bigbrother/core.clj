(ns bigbrother.core
  "This file provide helpers to manage time spend in functions"
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [overtone.at-at :refer [every mk-pool]]
            [riemann.client :as r]
            [clojure.algo.generic.functor :refer [fmap]]

            [bigbrother.timer :as timer]
            [bigbrother.counter :as counter]
            [bigbrother.metrics :as metrics]
            [bigbrother.max-metrics :as max-metrics]
            ))

;; Atoms
(def pool (atom nil)) ;; pool for async
(def default-map (atom #{}))
(def riemann-conf (atom nil))
(def riemann-conn (atom nil))
(def riemann-service (atom "supercell"))
(def level-by-key (atom nil))

(def n (atom 0))      ;; a number

(def log-time timer/log-time)
(def log-counter counter/log-counter)
(def log-metric metrics/log-metric)
(def log-mmetric max-metrics/log-mmetric)

(defn timer-loop-finished []
  ;; increment the number of loop
  (swap! n inc)
  (timer/finish-timer-loop)
  (max-metrics/loop-finished)
  (metrics/loop-finished)
  (counter/loop-finished))

;; ---- aliases
(defn big-brother-is-watching-you "Starting the timer" []
  (timer/log-time :start))
(def telescreen-on "Starting the timer" big-brother-is-watching-you)

(def welcome-in-miniluv "End the timer chrono" timer-loop-finished)
(def telescreen-off "End the timer chrono" timer-loop-finished)

;; ------------------------------------------------------------------------------
;; Riemann Warn Level

(defn warn-level
  "warn level between two numbers"
  [warn crit]
  (fn [v] (cond (neg? v) "critical"
                (>= v crit) "critical"
                (>= v warn) "warning"
                :else "ok")))
(defn rev-warn-level
  [warn crit]
  (fn [v] (cond (<= v crit) "critical"
                (<= v warn) "warning"
                :else "ok")))

(defn ok [] (fn [_] "ok"))
(defn warning [] (fn [_] "warning"))
(defn critical [] (fn [_] "critical"))

(defn- to-riemann-event [[k v]]
  (when (number? v)
    (let [lvl-fn (get @level-by-key k)
          level  (if lvl-fn (lvl-fn v) "ok")]
      (into
        @riemann-conf
        {:service (str @riemann-service " " (name k))
         :state level 
         :metric v}))))

(defn send-to-riemann [m]
  (let [result-map (into @default-map m)
        events (remove nil? (map to-riemann-event result-map))]
    (when @riemann-conn
      (r/send-events @riemann-conn events))))


(defn reset-accumulators! []
  (counter/reset-acc!)
  (metrics/reset-acc!)
  (max-metrics/reset-acc!)
  (timer/reset-acc!)
  (reset! n 0))

(defn reset-all-atoms! []
  (reset! timer/times [])
  (reset! metrics/metrics {})
  (reset! max-metrics/mmetrics {})
  (reset! counter/counters {})
  (reset-accumulators!))

(defn resume-map [nb-ms]
  (let [n-by-sec  (float (/ @n (/ nb-ms 1000)))
        basic     {:nb n-by-sec}]
    (reduce into basic [(metrics/resume @n)
                        (max-metrics/resume)
                        (timer/resume)
                        (counter/resume nb-ms)])))

(defn display-time
  "display the time at most every 10s"
  [nb-ms]
  (let [result (resume-map nb-ms)]
    (log/info (json/write-str result))
    (send-to-riemann result)
    (reset-accumulators!)))

(defn init-metrics
  "## init-metrics

    init-map :: Map Keyword (v -> ERROR_LEVEL)"
  [init-map nb-ms-metrics riemann-host riemann-service-name riemann-default]
  (reset! default-map (reduce into {}
                              (map (fn [k] {k -1} )
                                   (conj (keys init-map) :total))))
  (reset! level-by-key init-map)
  (reset! pool (mk-pool))
  (reset! riemann-conf riemann-default)
  (reset! riemann-service riemann-service-name)
  (when riemann-host
    (reset! riemann-conn (r/tcp-client {:host riemann-host})))
  (every nb-ms-metrics (fn [] (display-time nb-ms-metrics)) @pool))
