(ns braid.emoji.client.events
  (:require
   [re-frame.core :refer [reg-event-fx]]
   [clojure.set :as set]))

(reg-event-fx
  :emoji/add-emoji
  (fn [_ [_ {:keys [group-id shortcode image] :as emoji}]]
    {:websocket-send
     (list [:braid.server.emoji/add-custom-emoji emoji])}))

(reg-event-fx
  :emoji/edit-emoji
  (fn [_ [_ emoji-id new-code]]
    {:websocket-send
     (list [:braid.server.emoji/edit-custom-emoji [emoji-id new-code]])}))

(reg-event-fx
  :emoji/retract-emoji
  (fn [_ [_ emoji-id]]
    {:websocket-send
     (list [:braid.server.emoji/retract-custom-emoji emoji-id])}))

(reg-event-fx
  :emoji/new-emoji-notification
  (fn [{db :db} [_ new-emoji]]
    {:db (assoc-in db [:braid.emoji.client.core/group-emoji
                       (new-emoji :group-id)
                       (new-emoji :shortcode)]
                   new-emoji)}))

(reg-event-fx
  :emoji/edit-emoji-notification
  (fn [{db :db} [_ group-id old-code new-code]]
    {:db (-> (update-in db [:braid.emoji.client.core/group-emoji group-id]
                        set/rename-keys {old-code new-code})
             (assoc-in [:braid.emoji.client.core/group-emoji group-id
                        new-code :shortcode] new-code))}))

(reg-event-fx
  :emoji/remove-emoji-notification
  (fn [{db :db} [_ group-id emoji-id]]
    {:db (update-in
           db
           [:braid.emoji.client.core/group-emoji group-id]
           (partial into {}
                    (remove (fn [[_ {id :id}]] (= id emoji-id)))))}))
