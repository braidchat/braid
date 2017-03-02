(ns braid.server.quests.sync
  (:require
    [braid.server.db :as db]
    [braid.server.socket :refer [chsk-send! connected-uids]]
    [braid.server.sync-handler :refer [event-msg-handler]]
    [braid.server.quests.db :as quests]))

(defmethod event-msg-handler :braid.server.quests/upsert-quest-record
  [{:keys [?data user-id]}]
  (chsk-send! user-id [:braid.client.quests/upsert-quest-record ?data])
  (db/run-txns! (quests/upsert-quest-record-txn user-id ?data)))
