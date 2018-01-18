(ns braid.core.api
  (:require
    [re-frame.core :as re-frame]))

(def dispatch re-frame/dispatch)
(def subscribe re-frame/subscribe)

(defn reg-event-fx [key handler]
  (re-frame/reg-event-fx key handler))

(defn reg-sub [key handler]
  (re-frame/reg-sub key handler))

; ---------

(reg-event-fx ::register-event-listener!
  (fn [_ [_ id listener]]
    (re-frame/add-post-event-callback id (fn [event _]
                                          (listener event)))
    {}))

(defn register-event-listener!
  [[id listener]]
  (dispatch [::register-event-listener! id listener]))
