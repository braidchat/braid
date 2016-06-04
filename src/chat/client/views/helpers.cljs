(ns chat.client.views.helpers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! put! chan alts! timeout]]
            [cljs-time.format :as f]
            [cljs-time.core :as t]
            [chat.client.store :as store]
            [goog.style :as gstyle]))

; TODO: clojure 1.8 should implement this:
(defn starts-with? [s prefix]
  ; not using .startsWith because it's only supported in ES6
  (= 0 (.indexOf s prefix)))

; TODO: clojure 1.8 should implement this:
(defn ends-with? [s suffix]
  (let [pos (- (count s) (count suffix))
        idx (.indexOf s suffix)]
    (and (not= -1 idx) (= idx pos))))

(defn decimal->color [decimal]
 (str "hsl(" (int (* 360 (mod decimal 1))) ",71%,35%)") )

(defn string->color [string]
  (-> string
      char-array
      (->> (map int)
           (apply +))
      (/ 213)
      decimal->color))

(defn id->color [uuid]
  (-> uuid
      str
      (.substring 33 36)
      (js/parseInt 16)
      (/ 4096)
      ; at this point, we have a decimal between 0 and 1
      ; approximately evenly distributed
      decimal->color))

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

(defn element-offset
  [elt]
  (let [offset (gstyle/getPageOffset elt)]
    [(.-x offset) (.-y offset)]))

(defn get-style
  [elt prop]
  (cond
    (.-currentStyle elt)
    (aget (.-currentStyle elt) prop)

    (.-getComputedStyle js/window)
    (.. js/document -defaultView
        (getComputedStyle elt nil)
        (getPropertyValue prop))))
