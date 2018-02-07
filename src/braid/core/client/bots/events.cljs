(ns braid.core.client.bots.events
  (:require
   [braid.core.client.schema :as schema]
   [cljs-uuid-utils.core :as uuid]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]))

(defn make-bot [data]
  (merge {:id (uuid/make-random-squuid)}
         data))

(reg-event-db
  :add-group-bot
  (fn [state [_ [group-id bot]]]
    (update-in state [:groups group-id :bots] conj bot)))

(reg-event-db
  :remove-group-bot
  (fn [state [_ [group-id bot-id]]]
    (update-in
      state
      [:groups group-id :bots]
      (partial into [] (remove (fn [b] (= bot-id (b :id))))))))

(reg-event-db
  :update-group-bot
  (fn [state [_ [group-id bot]]]
    (update-in
      state [:groups group-id :bots]
      (partial mapv (fn [b] (if (= (b :id) (bot :id))
                              (merge b bot)
                              b))))))

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
                           "Something went wrong creating bot")]]))
           (on-complete (:braid/ok reply))))})))

(reg-event-fx
  :retract-bot
  (fn [cofx [_ {:keys [bot-id]}]]
    {:websocket-send
     (list [:braid.server/retract-bot bot-id]
           5000
           (fn [reply]
             (when (nil? (:braid/ok reply))
               (dispatch [:display-error
                          [(str "bot-" bot-id (rand))
                           (get reply :braid/error
                                "Something went wrong retract bot")]]))))}))

(reg-event-fx
  :edit-bot
  (fn [cofx [_ {:keys [bot on-complete]}]]
    {:websocket-send
     (list
       [:braid.server/edit-bot bot]
       5000
       (fn [reply]
         (when-not (:braid/ok reply)
           (dispatch [:display-error
                      [(str "bot-" (bot :id) (rand))
                       (get reply :braid/error
                            "Something went wrong when updating bot")]]))
         (on-complete (:braid/ok reply))))}))

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
