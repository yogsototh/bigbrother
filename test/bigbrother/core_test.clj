(ns bigbrother.core-test
  (:require
   [bigbrother.core :refer :all]

   [clojure.test :refer :all]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :refer [defspec]]))

(deftest timer-test
  (do
    (reset-all-atoms!)
    ;; first loop
    (telescreen-on)
    (Thread/sleep 30)
    (log-time :x1)
    (Thread/sleep 30)
    (log-time :x2)
    (Thread/sleep 30)
    (log-time :end)
    (telescreen-off)
    ;; second loop
    (log-time :start)
    (Thread/sleep 30)
    (log-time :x2)
    (Thread/sleep 30)
    (log-time :end)
    (telescreen-off)
    (let [result (resume-map 1000)]
      (is (contains? result :nb))
      (is (contains? result :x1))
      (is (contains? result :x2))
      (is (>= (:total result) (:x2 result))))))

(deftest check-metrics
  (do
    (reset-all-atoms!)
    ;; first loop
    (log-metric :foo 1)
    (log-metric :bar 3)
    (timer-loop-finished)
    (log-metric :foo 2)
    (log-metric :bar 2)
    (timer-loop-finished)
    (log-metric :foo 3)
    (log-metric :bar 1)
    (timer-loop-finished)
    (let [result (resume-map 1000)]
      (is (contains? result :foo))
      (is (contains? result :bar))
      (is (contains? result :nb))
      (is (= 2.0 (:foo result)))
      (is (= 2.0 (:bar result)))
      (is (= 3.0 (:nb result))))))

(deftest check-mmetrics
  (do
    (reset-all-atoms!)
    (log-mmetric :foo 80)
    (log-mmetric :bar 4)
    (timer-loop-finished)
    (log-mmetric :foo 50)
    (log-mmetric :bar 5)
    (timer-loop-finished)
    (log-mmetric :foo 60)
    (log-mmetric :bar 6)
    (timer-loop-finished)
    (let [result (resume-map 1000)]
      (is (contains? result :foo))
      (is (contains? result :bar))
      (is (contains? result :nb))
      (is (= 80 (:foo result)))
      (is (= 6  (:bar result)))
      (is (= 3.0 (:nb result))))))

(deftest check-counters
  (do
    (reset-all-atoms!)
    ;; first loop
    (log-counter :foo)
    (log-counter :foo)
    (log-counter :bar 3)
    (timer-loop-finished)
    (log-counter :foo)
    (log-counter :bar 2)
    (timer-loop-finished)
    (log-counter :foo)
    (log-counter :bar 1)
    (timer-loop-finished)
    (let [result (resume-map 1000)]
      (is (contains? result :foo))
      (is (= 4.0 (:foo result)))
      (is (= 6.0 (:bar result)))
      (is (= 3.0 (:nb result))))))

(defspec check-all-keys-exists
  30
  (prop/for-all
   [actions (gen/such-that #(> (count %) 1)
                           (gen/vector gen/keyword))]
   (do
     (reset-all-atoms!)
     (doall (map (fn [a] (do (log-time a) (Thread/sleep 10)))
                 actions))
     (timer-loop-finished))
   (let [result (resume-map 1000)]
     (every? #(contains? result %) (rest actions)))))

(defspec check-total-is-greater-than-each-input
  30
  (prop/for-all
   [actions (gen/such-that #(> (count %) 1)
                           (gen/vector gen/keyword))]
   (do
     (reset-all-atoms!)
     (doall (map (fn [a] (do (log-time a) (Thread/sleep 10)))
                 actions))
     (timer-loop-finished))
   (let [result (resume-map 1000)]
     (every? #(>= (:total result) (get result %)) (rest actions)))))
