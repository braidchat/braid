(ns braid.core.api
  (:require
    [re-frame.core :as re-frame]))

(def dispatch re-frame/dispatch)
(def reg-event-fx re-frame/reg-event-fx)
(def reg-sub re-frame/reg-sub)

(reg-event-fx :braid.core/init-state
  (fn [{db :db} [_ state]]
    {:db (merge db state)}))
