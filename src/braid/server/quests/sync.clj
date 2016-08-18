(ns braid.server.quests.sync
  (:require [braid.server.sync-handler :refer [event-msg-handler]]
            [braid.server.db :as db]))

(defmethod event-msg-handler :braid.server.quests/skip-quest
  [{:keys [?data user-id]}]
  (db/skip-quest! user-id ?data))

(defmethod event-msg-handler :braid.server.quests/complete-quest
  [{:keys [?data user-id]}]
  (db/complete-quest! user-id ?data))

(defmethod event-msg-handler :braid.server.quests/increment-quest
  [{:keys [?data user-id]}]
  (db/increment-quest! user-id ?data))

(defmethod event-msg-handler :braid.server.quests/store-quest-record
  [{:keys [?data user-id]}]
  (db/store-quest-record! user-id ?data))
