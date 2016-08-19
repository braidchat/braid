(ns braid.server.quests.sync
  (:require [braid.server.sync-handler :refer [event-msg-handler]]
            [braid.server.db :as db]
            [braid.server.socket :refer [chsk-send! connected-uids]]))

(defmethod event-msg-handler :braid.server.quests/upsert-quest-record
  [{:keys [?data user-id]}]
  (chsk-send! user-id [:braid.client.quests/upsert-quest-record ?data])
  (db/upsert-quest-record! user-id ?data))
