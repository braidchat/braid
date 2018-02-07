(ns braid.quests.client.remote-handlers
  (:require
   [braid.core.client.sync :as sync]
   [re-frame.core :refer [dispatch]]))

(defmethod sync/event-handler :braid.quests/upsert-quest-record
  [[_ quest-record]]
  (dispatch [:quests/upsert-quest-record quest-record]))
