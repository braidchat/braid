(ns braid.client.dispatcher
  (:require [re-frame.core :as re-frame]))

(defn dispatch!
  ([event args]
   (println event)
   (re-frame/dispatch [event args]))
  ([event]
   (dispatch! event nil)))
