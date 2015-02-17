# Big Brother

A Clojure library designed to monitor things and retrieve some metrics.

It could send the metrics to [Riemann](http://riemann.io).

Add this dependency to your `project.clj`:

~~~
[bigbrother "0.1.0"]
~~~

## Usage

Full example:

~~~ {.clojure}
(ns myproject.core
    (:require [bigbrother.core :as bb]))

...
(def riemann-host "localhost")
(def riemann-service "bb")
(def nb-ms-metrics 1000) ; send metrics info every second
(bb/init-metrics
    {:read-book     (bb/warn-level 10 100) ; <- less than 10ms is ok
     :write-notes   (bb/warn-level 10 100) ; <- warn between 10ms and 100ms
     :feel-free     (bb/warn-level 10 100) ; <- critical if more than 100ms
     :free-time     (bb/warn-level 10 100) ; <- normal if less than 10ms
     :work-as-usual (bb/rev-warn-level 100 10) ; <- normal if more than 100 critical <10
     :total         (bb/warn-level 100 500) ; <- 0..100 normal, 0..500 warn, >500 critical
     :nb            (bb/rev-warn-level 5 1) <- less than 1 critical, less than 5 warn
     }
    nb-ms-metrics
    riemann-host
    riemann-service)

;; If you log metrics not declared in init-metrics, they will show as always "ok"

...

(defn function-called-frequently
    []
    (bb/big-brother-is-watching-you) ; <-- you must start the timer
    (when (has-book?)
        (read-book) ; <-- do your action
        (bb/log-time :read-book) ; <-- tell the timer you've done something
        (bb/log-counter :book) ; <-- increment the counter :book
        )
    (write-notes)
    (bb/log-time :write-notes)
    (let [time-felt-free (feel-free)]
        (bb/log-time :feel-free)
        (bb/log-mmetric :freetime time-felt-free))
    (work-as-usual)
    (bb/log-time :work-as-usual)
    (bb/telescreen-off)) ; <-- end the timer loop to flush times.
~~~

### Init

The `init-metrics` function takes:

- a map containing metrics name for keys and warning level functions as values
- the nb of ms to wait before sending new metrics
- a riemann host (if `nil` won't use riemann)
- a riemann service name.

That will send data to riemann every nb-ms-metrics ms.

~~~
(bb/init-metrics
    {:read-book     (bb/warn-level 10 100) ; <- less than 10ms is ok
     :write-notes   (bb/warn-level 10 100) ; <- warn between 10ms and 100ms
     :feel-free     (bb/warn-level 10 100) ; <- critical if more than 100ms
     :free-time     (bb/warn-level 10 100) ; <- normal if less than 10ms
     :work-as-usual (bb/rev-warn-level 100 10) ; <- normal if more than 100 critical <10
     :total         (bb/warn-level 100 500) ; <- 0..100 normal, 0..500 warn, >500 critical
     :nb            (bb/rev-warn-level 5 1) <- less than 1 critical, less than 5 warn
     }
    nb-ms-metrics
    riemann-host
    riemann-service)
~~~

### Timers

~~~
(do
    (action)
    (bb/log-time :action))
~~~

Will tell big brother that the action :action finished.
Generally you use timers like this:

~~~
(do
    (bb/big-brother-is-watching-you)
    (action1)
    (bb/log-time :action1)
    (action1)
    (bb/log-time :action1)
    (bb/telescreen-off))
~~~

You declare a start time which will start the chrono.
Then each time you decalare a `log-time` it will add the time taken to the action name.

Once you finished your course of action you declare the timer-loop as finished.
And you stop the chrono up until the next `(bb/big-brother-is-watching-you)`.

In the end, the value will be the mean of the time taken by each action during the
big loop.

### Counters

Counter can be called:

~~~
(do
  (bb/log-counter :foo)
  (bb/log-counter :bar 3))
~~~

Will increment :foo by 1 and :bar by 3.
In the end, the result will be the mean of the counter number by second.

### Metrics

Metrics are quite similar to counters.
The main difference is that there is no notion of increment.
You must always provide a value for a metrics.
The resume will contains its mean normalized by second.

~~~
(do
    (bb/log-metric :foo 0.5)
    (bb/log-metric :foo 2.5)
    (bb/log-metric :foo 3))
~~~

In the resume you'll get `{:foo 2.0}` (`(0.5 + 2.5 + 3)/3`).


### Max Metrics

Sometime you might want to get the maximum of some values during an interval of time.

~~~
(do
  (bb/log-mmetric :foo 5)
  (bb/log-mmetric :foo 1)
  (bb/log-mmetric :foo 3))
~~~

That will display `{:foo 5}` in the resume.

## License

Copyright © 2015 Yann Esposito

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
