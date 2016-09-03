(ns braid.client.group-admin.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx
  :set-group-intro
  (fn [{state :db :as cofx} [_ {:keys [group-id intro local-only?] :as args}]]
    {:db (assoc-in state [:groups group-id :intro] intro)
     :websocket-send (when-not local-only?
                       (list [:braid.server/set-group-intro args]))}))

(reg-event-fx
  :set-group-avatar
  (fn [{state :db :as cofx} [_ {:keys [group-id avatar local-only?] :as args}]]
    {:db (assoc-in state [:groups group-id :avatar] avatar)
     :websocket-send (when-not local-only?
                       (list [:braid.server/set-group-avatar args]))}))

(reg-event-fx
  :make-group-public!
  (fn [cofx [_ group-id]]
    {:websocket-send (list [:braid.server/set-group-publicity [group-id true]])}))

(reg-event-fx
  :make-group-private!
  (fn [cofx [_ group-id]]
    {:websocket-send (list [:braid.server/set-group-publicity [group-id false]])}))

(reg-event-db
  :set-group-publicity
  (fn [state [_ [group-id publicity]]]
    (assoc-in state [:groups group-id :public?] publicity)))
