(ns chat.client.views.helpers
  (:require [om.core :as om]
            [cljs-time.format :as f]
            [cljs-time.core :as t]
            [chat.client.store :as store]))

(defn user-cursor
  "Get an om cursor for the given user"
  [user-id]
  (when (store/valid-user-id? user-id)
    (om/ref-cursor (get-in (om/root-cursor store/app-state) [:users user-id]))))

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
