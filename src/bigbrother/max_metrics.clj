(ns bigbrother.max-metrics)

;; ------------------------------------------------------------------------------
;; Max Metrics

;; ## Monoid instance of `maxmetrics`
;; a sum time is a list of couple `{name metric-value}`
(def empty-maxmetric {})
(defn add-maxmetrics [st st2]
  (merge-with max st st2))

(def mmetrics (atom {})) ;; timestamps
(def maxmetrics (atom empty-maxmetric)) ;; time spent by type

;; Max Metrics (metrics to do a max between them instead of a mean)
(defn- set-mmetric! [k v] (swap! mmetrics add-maxmetrics {k v}))
(defn log-mmetric
  "declare a specific max metrics (not time)"
  [k v]
  (set-mmetric! k v))
(defn log-mmetric->
  "declare a specific max metrics (not time) and returns the first parameter"
  [x k v]
  (log-mmetric k v)
  x)

(defn loop-finished []
  ;; aggreate maxmetrics
  (swap! maxmetrics add-maxmetrics @mmetrics)
  (reset! mmetrics {}))

(defn reset-acc! []
  (reset! maxmetrics empty-maxmetric))

(defn resume [] @maxmetrics)
