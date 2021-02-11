(ns braid.bots.client.events
  (:require
   [braid.core.client.schema :as schema]
   [braid.base.client.state :refer [reg-event-fx]] ;; TODO should use base.api/register-events!
   [cljs-uuid-utils.core :as uuid]
   [re-frame.core :refer [dispatch]]))

(defn make-bot [data]
  (merge {:id (uuid/make-random-squuid)}
         data))

(def events
  {:bots/add-group-bot!
   (fn [{db :db} [_ [group-id bot]]]
     {:db (update-in db [:groups group-id :bots] conj bot)})

   :bots/remove-group-bot!
   (fn [{db :db} [_ [group-id bot-id]]]
     {:db (update-in
            db
            [:groups group-id :bots]
            (partial into [] (remove (fn [b] (= bot-id (b :id))))))})

   :bots/update-group-bot!
   (fn [{db :db} [_ [group-id bot]]]
     {:db
      (update-in
        db [:groups group-id :bots]
        (partial mapv (fn [b] (if (= (b :id) (bot :id))
                               (merge b bot)
                               b))))})

   :bots/new-bot!
   (fn [_ [_ {:keys [bot on-complete]}]]
     (let [bot (make-bot bot)]
       {:websocket-send
        (list
          [:braid.server/create-bot bot]
          5000
          (fn [reply]
            (when (nil? (:braid/ok reply))
              (dispatch [:braid.notices/display!
                         [(keyword "bot-creation-error" (bot :id))
                          (get reply :braid/error
                               "Something went wrong creating bot")
                          :error]]))
            (on-complete (:braid/ok reply))))}))

   :bots/retract-bot!
   (fn [_ [_ {:keys [bot-id]}]]
     {:websocket-send
      (list [:braid.server/retract-bot bot-id]
            5000
            (fn [reply]
              (when (nil? (:braid/ok reply))
                (dispatch [:braid.notices/display!
                           [(keyword "bot-retraction-error" bot-id)
                            (get reply :braid/error
                                 "Something went wrong retract bot")
                            :error]]))))})

   :bots/edit-bot!
   (fn [_ [_ {:keys [bot on-complete]}]]
     {:websocket-send
      (list
        [:braid.server/edit-bot bot]
        5000
        (fn [reply]
          (when-not (:braid/ok reply)
            (dispatch [:braid.notices/display!
                       [(keyword "bot-edit" (bot :id))
                        (get reply :braid/error
                             "Something went wrong when updating bot")
                        :error]]))
          (on-complete (:braid/ok reply))))})

   :bots/get-bot-info!
   (fn [_ [_ {:keys [bot-id on-complete]}]]
     {:websocket-send
      (list
        [:braid.server/get-bot-info bot-id]
        2000
        (fn [reply]
          (when-let [bot (:braid/ok reply)]
            (on-complete bot))))})})
