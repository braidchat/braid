(ns braid.custom-emoji.client.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :custom-emoji/group-emojis
  (fn [db [_ group-id]]
    (vals (get-in db [:custom-emoji/group-emoji
                      (or group-id (db :open-group-id))]))))

(reg-sub
  :custom-emoji/custom-emoji
  (fn [db [_ shortcode]]
    (get-in db [:custom-emoji/group-emoji
                (db :open-group-id)
                shortcode])))
