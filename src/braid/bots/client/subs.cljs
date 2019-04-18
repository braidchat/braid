(ns braid.bots.client.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(def subs
  {:bots/group-bots
   (fn [state _ [group-id]]
     (get-in state [:groups group-id :bots]))})
