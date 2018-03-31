(ns braid.emoji.client.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  :emoji/custom-emoji
  (fn [db [_ group-id]]
    (vals (get-in db [:braid.emoji.client.core/group-emoji group-id]))))
