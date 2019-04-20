(ns braid.bots.client.subs
  (:refer-clojure :exclude [subs])
  (:require
   [re-frame.core :refer [reg-sub]]))

(def subs
  {:bots/group-bots
   (fn [state _ [group-id]]
     (get-in state [:groups group-id :bots]))

   :bots/group-bot
   (fn [{:keys [open-group-id] :as state} [_ bot-user-id]]
     (let [group-bots (get-in state [:groups open-group-id :bots])]
       (some-> group-bots
               (->> (filter (fn [b] (= (b :user-id) bot-user-id))))
               first)))})
