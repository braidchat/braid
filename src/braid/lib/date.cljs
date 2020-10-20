(ns braid.lib.date
  (:require
   [cljs-time.core :as t]
   [cljs-time.format :as f]))

(defn format-date
  [format-string datetime]
  (f/unparse (f/formatter format-string) (t/to-default-time-zone datetime)))

(defn smart-format-date
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
