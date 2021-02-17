(ns braid.quests.client.remote-handlers
  (:require
   [braid.base.client.socket :as socket] ;; FIXME should register! not use directly
   [re-frame.core :refer [dispatch]]))

(defmethod socket/event-handler :braid.quests/upsert-quest-record
  [[_ quest-record]]
  (dispatch [:quests/upsert-quest-record! quest-record]))
