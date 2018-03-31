(ns braid.emoji.client.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  :emoji/group-emojis
  (fn [db [_ group-id]]
    (vals (get-in db [:braid.emoji.client.core/group-emoji
                      (or group-id (db :open-group-id))]))))

(reg-sub
  :emoji/custom-emoji
  (fn [db [_ shortcode]]
    (get-in db [:braid.emoji.client.core/group-emoji
                (db :open-group-id)
                shortcode])))
