(ns braid.bots.client.remote-handlers
  (:require
   [re-frame.core :refer [dispatch]]))

(def handlers
  {:braid.client.bots/new-bot
   (fn [_ [group-id bot]]
     (dispatch [:bots/add-group-bot! [group-id bot]]))

   :braid.client.bots/retract-bot
   (fn [_ [group-id bot-id]]
     (dispatch [:bots/remove-group-bot! [group-id bot-id]]))

   :braid.client.bots/edit-bot
   (fn [_ [group-id bot]]
     (dispatch [:bots/update-group-bot! [group-id bot]]))})
