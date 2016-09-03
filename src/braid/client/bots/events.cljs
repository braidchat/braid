(ns braid.client.bots.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [braid.client.schema :as schema]
            [cljs-uuid-utils.core :as uuid]))

(defn make-bot [data]
  (merge {:id (uuid/make-random-squuid)}
         data))

(reg-event-db
  :add-group-bot
  (fn [state [_ [group-id bot]]]
    (update-in state [:groups group-id :bots] conj bot)))

(reg-event-fx
  :new-bot
  (fn [cofx [_ {:keys [bot on-complete]}]]
    (let [bot (make-bot bot)]
      {:websocket-send
       (list
         [:braid.server/create-bot bot]
         5000
         (fn [reply]
           (when (nil? (:braid/ok reply))
             (dispatch [:display-error
                        [(str "bot-" (bot :id) (rand))
                         (get reply :braid/error
                           "Something when wrong creating bot")]]))
           (on-complete (:braid/ok reply))))})))

(reg-event-fx
  :get-bot-info
  (fn [cofx [_ {:keys [bot-id on-complete]}]]
    {:websocket-send
     (list
       [:braid.server/get-bot-info bot-id]
       2000
       (fn [reply]
         (when-let [bot (:braid/ok reply)]
           (on-complete bot))))}))
