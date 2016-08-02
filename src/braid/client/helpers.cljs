(ns braid.client.helpers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! put! chan alts! timeout]]
            [clojure.string :as string]
            [cljs-time.format :as f]
            [cljs-time.core :as t]
            [goog.style :as gstyle]
            [cljsjs.husl])
  (:import [goog Uri]))

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

(def url-re #"(http(?:s)?://\S+(?:\w|\d|/))")

(defn extract-urls
  "Given some text, returns a sequence of URLs contained in the text"
  [text]
  (map first (re-seq url-re text)))

(defn contains-urls? [text]
  (boolean (seq (extract-urls text))))

(defn url->parts [url]
  (let [url-info (.parse Uri url)]
    {:domain (.getDomain url-info)
     :path (.getPath url-info)
     :scheme (.getScheme url-info)
     :port (.getPort url-info)}))

(defn site-url
  []
  (let [{:keys [domain scheme port]} (url->parts (.-location js/window))]
    (str scheme "://" domain (when (or (and (= scheme "http")
                                       (not= port 80))
                                     (and (= scheme "https")
                                       (not= port 443)))
                             (str ":" port)))))

(defn ->color [input]
  (js/window.HUSL.toHex (mod (Math/abs (hash input)) 360) 95 50))

(defn id->color [uuid]
  (->color uuid))

(defn url->color [url]
  (-> url
      string/lower-case
      url->parts
      :domain
      ->color))

(defn stop-event! [e]
  (.stopPropagation e)
  (.preventDefault e))
