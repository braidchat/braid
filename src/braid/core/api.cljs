(ns braid.core.api
  (:require
    [re-frame.core :as re-frame]))

(enable-console-print!)

(def dispatch re-frame/dispatch)
(def subscribe re-frame/subscribe)

(defn reg-event-fx [key handler]
  (println "api event:" key)
  (re-frame/reg-event-fx key handler))

(defn reg-sub [key handler]
  (println "api sub:" key)
  (re-frame/reg-sub key handler))
