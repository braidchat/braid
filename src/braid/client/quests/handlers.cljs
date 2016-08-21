(ns braid.client.quests.handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [braid.client.quests.helpers :as helpers]
            [cljs-uuid-utils.core :as uuid]))

(defn make-quest-record [{:keys [quest-id]}]
  {:quest-record/id (uuid/make-random-squuid)
   :quest-record/state :active
   :quest-record/progress 0
   :quest-record/quest-id quest-id})

(reg-event-fx
  :quests/activate-next-quest
  (fn [{state :db :as cofx} _]
    (if-let [quest (helpers/get-next-quest state)]
      (let [quest-record (make-quest-record {:quest-id (quest :quest/id)})
            state (helpers/store-quest-record state quest-record)]
        {:db state
         :websocket-send (list [:braid.server.quests/upsert-quest-record quest-record])})
      {})))

(reg-event-fx
  :quests/skip-quest
  (fn [state [_ quest-record-id]]
    (let [state (helpers/skip-quest state quest-record-id)]
      {:websocket-send
       (list [:braid.server.quests/upsert-quest-record
              (helpers/get-quest-record state quest-record-id)])
       :dispatch [:activate-next-quest]})))

(reg-event-fx
  :quests/complete-quest
  (fn [{state :db :as cofx} [_ quest-record-id]]
    (let [state (helpers/complete-quest state quest-record-id)]
      {:websocket-send
       (list [:braid.server.quests/upsert-quest-record
              (helpers/get-quest-record state quest-record-id)])
       :dispatch [:activate-next-quest]})))

(reg-event-fx
  :quests/increment-quest
  (fn [{state :db :as cofx} [_ quest-record-id]]
    (let [state (helpers/increment-quest state quest-record-id)]
      {:db state
       :websocket-send (list [:braid.server.quests/upsert-quest-record
                              (helpers/get-quest-record state quest-record-id)])})))

(reg-event-db
  :quests/upsert-quest-record
  (fn [state [_ quest-record]]
    (helpers/store-quest-record state quest-record)))
