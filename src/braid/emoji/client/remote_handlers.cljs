(ns braid.emoji.client.remote-handlers
  (:require
   [braid.core.client.sync :as sync]
   [re-frame.core :refer [dispatch]]))

(defmethod sync/event-handler :braid.emoji/new-emoji-notification
  [[_ new-emoji]]
  (dispatch [:emoji/new-emoji-notification new-emoji]))
