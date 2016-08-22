(ns braid.client.quests.remote-handlers
  (:require [re-frame.core :refer [dispatch]]
            [braid.client.sync :as sync]))

(defmethod sync/event-handler :braid.client.quests/upsert-quest-record
  [[_ quest-record]]
  (dispatch [:quests/upsert-quest-record quest-record]))
