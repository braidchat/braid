(ns braid.core.client.helpers
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<! put! chan alts! timeout]]
   [goog.style :as gstyle]))

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

(defn stop-event! [e]
  (.stopPropagation e)
  (.preventDefault e))
