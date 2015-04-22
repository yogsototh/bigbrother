(ns bigbrother.timer)

;; ------------------------------------------------------------------------------
;; TIMERS

;; ## Monoid instance of `sumtimes`
;; a sum time is a list of couple `[name [timespent nb]]`
(defn ts-name [x] (first x))
(defn ts-timespent [x] (first (second x)))
(defn ts-nb [x] (second (second x)))

(def empty-sumtime [])
(defn- add-one-sumtime [st st2]
  [(ts-name st) [(+ (ts-timespent st) (ts-timespent st2))
                 (+ (ts-nb st) (ts-nb st2))]])
(defn add-sumtimes [st st2]
  (cond (empty? st) st2
        (empty? st2) st
        :else (doall (map add-one-sumtime st st2))))

(defn fmap-sumtimes [f st]
  (map (fn [v] [(first v) [(f (ts-timespent v))
                           (ts-nb v)]]) st))

(defn normalized-map-from-sumtimes
  "Given a `sumtimes` returns a map `{key timespent}`'"
  [st]
  (reduce #(merge-with + %1 %2) {}
          (map (fn [v] {(ts-name v)
                        (/ (ts-timespent v)
                           (ts-nb v))}) st)))
;; timers atoms
(def times (atom {})) ;; {session [timestamps]}
(def sumtimes (atom empty-sumtime)) ;; time spent by type

;; Timer
(defn- set-value! [session k v]
  (let [update-one-raw #(if (nil? %) [[k v]] (conj % [k v]))]
    (swap! times #(update-in % [session] update-one-raw))))

(defn log-time
  "declare the action named `k` finished"
  [session k]
  (set-value! session k (System/nanoTime)))

(defn- show-one-step
  "get time spent during one step"
  [x]
  (if (< (count x) 2)
    {:nothing 0}
    (let [from (second (first x))
          k    (first  (second x))
          to   (second (second x))]
      [k [(- to from) 1]])))

(defn timespent
  "from a list of timestamp generate a sumtime"
  [times-array]
  (map show-one-step (partition 2 1 times-array)))

(defn total-time
  "from a list of timestamp generate the total time spent"
  [times-array]
  (- (second (last times-array))
     (second (first times-array))))

(defn to-milliseconds
  "convert from nanoseconds to milliseconds"
  [times-array]
  (fmap-sumtimes #(float (/ % 1000000)) times-array))

(defn finish-timer-loop [session]
  ;; Convert actual timestamps to sumtimes and aggregate them
  (let [ts (get @times session)]
    (if (> (count ts) 1)
      (let [difftime (timespent ts)
            total    (total-time ts)
            res      (to-milliseconds (conj difftime [:total [total 1]]))]
        (swap! sumtimes add-sumtimes res))))
  (swap! times assoc session []))

(defn end-session! [session]
  (swap! times dissoc session))

(defn reset-acc! []
  (reset! sumtimes empty-sumtime))

(defn resume []
  (normalized-map-from-sumtimes @sumtimes))
