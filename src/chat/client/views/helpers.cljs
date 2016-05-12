(ns chat.client.views.helpers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! put! chan alts! timeout]]
            [cljs-time.format :as f]
            [cljs-time.core :as t]
            [chat.client.store :as store]))

; TODO: clojure 1.8 should implement these
(defn starts-with? [s prefix]
  ; not using .startsWith because it's only supported in ES6
  (= 0 (.indexOf s prefix)))
(defn ends-with? [s suffix]
  (let [pos (- (count s) (count suffix))
        idx (.indexOf s suffix)]
    (and (not= -1 idx) (= idx pos))))

(defn id->color [id]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> id
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",70%,35%)")))

(defn format-date
  "Turn a Date object into a nicely formatted string"
  [datetime]
  (let [datetime (t/to-default-time-zone datetime)
        now (t/to-default-time-zone (t/now))
        format (cond
                 (= (f/unparse (f/formatter "yyyydM") now)
                    (f/unparse (f/formatter "yyyydM") datetime))
                 "h:mm A"

                 (= (t/year now) (t/year datetime))
                 "h:mm A MMM d"

                 :else
                 "h:mm A MMM d yyyy")]
    (f/unparse (f/formatter format) datetime)))

(defn debounce
  "Given the input channel source and a debouncing time of msecs, return a new
  channel that will forward the latest event from source at most every msecs
  milliseconds"
  [source msecs]
  (let [out (chan)]
    (go
      (loop [state ::init
             lastv nil
             chans [source]]
        (let [[_ threshold] chans]
          (let [[v sc] (alts! chans)]
            (condp = sc
              source (recur ::debouncing v
                            (case state
                              ::init (conj chans (timeout msecs))
                              ::debouncing (conj (pop chans) (timeout msecs))))
              threshold (do (when lastv
                              (put! out lastv))
                            (recur ::init nil (pop chans))))))))
    out))

(defn location
  [e]
  [(.-clientX e) (.-clientY e)])

