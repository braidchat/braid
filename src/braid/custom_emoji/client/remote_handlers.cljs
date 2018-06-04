(ns braid.custom-emoji.client.remote-handlers
  (:require
   [re-frame.core :refer [dispatch]]
   [braid.core.client.sync :as sync]))

(defmethod sync/event-handler :custom-emoji/new-emoji-notification
  [[_ new-emoji]]
  (dispatch [:custom-emoji/new-emoji-notification new-emoji]))

(defmethod sync/event-handler :custom-emoji/remove-emoji-notification
  [[_ [group-id emoji-id]]]
  (dispatch [:custom-emoji/remove-emoji-notification group-id emoji-id]))

(defmethod sync/event-handler :custom-emoji/edit-emoji-notification
  [[_ [group-id old-code new-code]]]
  (dispatch [:custom-emoji/edit-emoji-notification group-id old-code new-code]))
