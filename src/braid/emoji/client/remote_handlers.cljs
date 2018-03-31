(ns braid.emoji.client.remote-handlers
  (:require
   [braid.core.client.sync :as sync]
   [re-frame.core :refer [dispatch]]))

(defmethod sync/event-handler :braid.emoji/new-emoji-notification
  [[_ new-emoji]]
  (dispatch [:emoji/new-emoji-notification new-emoji]))

(defmethod sync/event-handler :braid.emoji/remove-emoji-notification
  [[_ [group-id emoji-id]]]
  (dispatch [:emoji/remove-emoji-notification group-id emoji-id]))
