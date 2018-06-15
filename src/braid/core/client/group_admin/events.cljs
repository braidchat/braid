(ns braid.core.client.group-admin.events
  (:require
    [braid.core.client.state :refer [reg-event-fx]]))

(reg-event-fx
  :set-group-intro
  (fn [{state :db} [_ {:keys [group-id intro local-only?] :as args}]]
    {:db (assoc-in state [:groups group-id :intro] intro)
     :websocket-send (when-not local-only?
                       (list [:braid.server/set-group-intro args]))}))

(reg-event-fx
  :set-group-avatar
  (fn [{state :db} [_ {:keys [group-id avatar local-only?] :as args}]]
    {:db (assoc-in state [:groups group-id :avatar] avatar)
     :websocket-send (when-not local-only?
                       (list [:braid.server/set-group-avatar args]))}))

(reg-event-fx
  :make-group-public!
  (fn [_ [_ group-id]]
    {:websocket-send (list [:braid.server/set-group-publicity [group-id true]])}))

(reg-event-fx
  :make-group-private!
  (fn [_ [_ group-id]]
    {:websocket-send (list [:braid.server/set-group-publicity [group-id false]])}))

(reg-event-fx
  :set-group-publicity
  (fn [{db :db} [_ [group-id publicity]]]
    {:db (assoc-in db [:groups group-id :public?] publicity)}))
