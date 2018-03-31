(ns braid.emoji.client.events
  (:require
   [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  :emoji/add-emoji
  (fn [_ [_ {:keys [group-id shortcode image] :as emoji}]]
    {:websocket-send
     (list [:braid.server.emoji/add-custom-emoji emoji])}))

(reg-event-fx
  :emoji/new-emoji-notification
  (fn [{db :db} [_ new-emoji]]
    {:db (assoc-in db [:braid.emoji.client.core/group-emoji
                       (new-emoji :group-id)
                       (new-emoji :shortcode)]
                   new-emoji)}))
