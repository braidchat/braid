(ns braid.client.quests.handlers
  (:require [braid.client.state.handler.core :refer [handler]]
            [braid.client.quests.helpers :as helpers]
            [braid.client.sync :as sync]
            [cljs-uuid-utils.core :as uuid]))

(defn fn-> [state f]
  (f state))

(defn make-quest-record [{:keys [quest-id]}]
  {:quest-record/id (uuid/make-random-squuid)
   :quest-record/state :active
   :quest-record/progress 0
   :quest-record/quest-id quest-id})

(defn activate-next-quest [state]
  (if-let [quest (helpers/get-next-quest state)]
    (let [quest-record (make-quest-record {:quest-id (quest :quest/id)})]
      (-> state
          (helpers/store-quest-record quest-record)
          (fn-> (fn [state]
                  (sync/chsk-send! [:braid.server.quests/upsert-quest-record quest-record])
                  state))))
    state))

(defmethod handler :quests/skip-quest [state [_ quest-record-id]]
  (-> state
      (helpers/skip-quest quest-record-id)
      (fn-> (fn [state]
              (sync/chsk-send! [:braid.server.quests/upsert-quest-record (helpers/get-quest-record state quest-record-id)])
              state))
      (activate-next-quest)))

(defmethod handler :quests/complete-quest [state [_ quest-record-id]]
  (-> state
      (helpers/complete-quest quest-record-id)
      (fn-> (fn [state]
              (sync/chsk-send! [:braid.server.quests/upsert-quest-record (helpers/get-quest-record state quest-record-id)])
              state))
      (activate-next-quest)))

(defmethod handler :quests/increment-quest [state [_ quest-record-id]]
  (-> state
      (helpers/increment-quest quest-record-id)
      (fn-> (fn [state]
              (sync/chsk-send! [:braid.server.quests/upsert-quest-record (helpers/get-quest-record state quest-record-id)])
              state))))

(defmethod handler :quests/upsert-quest-record [state [_ quest-record]]
  (-> state
      (helpers/store-quest-record quest-record)))
