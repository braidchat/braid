(ns braid.core.api
  (:require
    [re-frame.core :as re-frame]))

(def ref
  (atom {:events []
         :subs []}))

(defn ^:export log []
  (cljs.pprint/pprint @ref))

(def dispatch re-frame/dispatch)
(def subscribe re-frame/subscribe)

(defn reg-event-fx [key handler]
  (swap! ref update :events conj key)
  (re-frame/reg-event-fx key handler))

(defn reg-sub [key handler]
  (swap! ref update :subs conj key)
  (re-frame/reg-sub key handler))

; ---------

(reg-event-fx :braid.core/register-event-listener!
  (fn [_ [_ id listener]]
    (re-frame/add-post-event-callback id (fn [event _]
                                          (listener event)))
    {}))
