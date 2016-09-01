(ns braid.client.bots.subs
  (:require [re-frame.core :refer [reg-sub ]]))

(reg-sub
  :group-bots
  (fn [state _ [group-id]]
    (get-in state [:groups group-id :bots])))


