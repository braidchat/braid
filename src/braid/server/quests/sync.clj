(ns braid.server.quests.sync
  (:require [braid.server.sync-handler :refer [event-msg-handler]]
            [braid.server.db :as db]
            [braid.server.socket :refer [chsk-send!]]))

(defmethod event-msg-handler :braid.server.quests/skip-quest
  [{:keys [?data user-id]}]
  (chsk-send! user-id [:braid.client.quests/skip-quest ?data])
  (db/skip-quest! user-id ?data))

(defmethod event-msg-handler :braid.server.quests/complete-quest
  [{:keys [?data user-id]}]
  (chsk-send! user-id [:braid.client.quests/complete-quest ?data])
  (db/complete-quest! user-id ?data))

(defmethod event-msg-handler :braid.server.quests/increment-quest
  [{:keys [?data user-id]}]
  (chsk-send! user-id [:braid.client.quests/increment-quest ?data])
  (db/increment-quest! user-id ?data))

(defmethod event-msg-handler :braid.server.quests/store-quest-record
  [{:keys [?data user-id]}]
  (chsk-send! user-id [:braid.client.quests/store-quest-record ?data])
  (db/store-quest-record! user-id ?data))
