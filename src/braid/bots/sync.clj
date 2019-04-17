(ns braid.bots.sync
  (:require
   [braid.bots.server :as bots]
   [braid.bots.db :as db]))

(defn notify-bots! [new-message]
  ; Notify bots mentioned in the message
  (when-let [bot-name (second (re-find #"^/(\w+)\b" (:content new-message)))]
    (when-let [bot (bot/bot-by-name-in-group bot-name (new-message :group-id))]
      (timbre/debugf "notifying bot %s" bot)
      (future (bots/send-message-notification bot new-message))))
  ; Notify bots subscribed to the thread
  (doseq [bot (bot/bots-watching-thread (new-message :thread-id))]
    (timbre/debugf "notifying bot %s" bot)
    (future (bots/send-message-notification bot new-message)))
  ; Notify bots subscribed to all messages
  (doseq [bot (bot/bots-for-message (new-message :group-id))]
    (when (thread/thread-has-tags? (new-message :thread-id))
      (future (bots/send-message-notification bot new-message)))))

(def server-message-handlers
  {:braid.server/create-bot
   (fn [{:as ev-msg :keys [?data  user-id]}]
     (let [bot ?data]
       (when (and (bot :group-id) (group/user-is-group-admin? user-id (bot :group-id)))
         (cond
           (not (re-matches util/bot-name-re (bot :name)))
           (do (timbre/warnf "User %s tried to create bot with invalid name %s"
                             user-id (bot :name))
               {:reply! {:braid/error "Bad bot name"}})

           (not (valid-url? (bot :webhook-url)))
           (do (timbre/warnf "User %s tried to create bot with invalid webhook url %s"
                             user-id (bot :webhook-url))
               {:reply! {:braid/error "Invalid webhook url for bot"}})

           (and (not (string/blank? (bot :event-webhook-url)))
                (not (valid-url? (bot :event-webhook-url))))
           (do (timbre/warnf "User %s tried to create bot with invalid event webhook url %s"
                             user-id (bot :event-webhook-url))
               {:reply! {:braid/error "Invalid event webhook url for bot"}})

           (not (string? (bot :avatar)))
           (do (timbre/warnf "User %s tried to create bot without an avatar"
                             user-id (bot :webhook-url))
               {:reply! {:braid/error "Bot needs an avatar image"}})

           :else
           (let [[created] (db/run-txns! (bot/create-bot-txn bot))]
             {:reply! {:braid/ok created}
              :group-broadcast! [(bot :group-id)
                                 [:braid.client/new-bot
                                  [(bot :group-id) (bot->display created)]]]})))))

   :braid.server/edit-bot
   (fn [{:as ev-msg bot :?data user-id :user-id}]
     (let [orig-bot (bot/bot-by-id (bot :id))]
       (when (group/user-is-group-admin? user-id (orig-bot :group-id))
         (cond
           (not (re-matches util/bot-name-re (bot :name)))
           (do (timbre/warnf "User %s tried to change bot to have an invalid name %s"
                             user-id (bot :name))
               {:reply! {:braid/error "Bad bot name"}})

           (not (valid-url? (bot :webhook-url)))
           (do (timbre/warnf "User %s tried to change bot to have invalid webhook url %s"
                             user-id (bot :webhook-url))
               {:reply! {:braid/error "Invalid webhook url for bot"}})

           (and (not (string/blank? (bot :event-webhook-url)))
                (not (valid-url? (bot :event-webhook-url))))
           (do (timbre/warnf "User %s tried to change bot to have invalid event webhook url %s"
                             user-id (bot :event-webhook-url))
               {:reply! {:braid/error "Invalid event webhook url for bot"}})

           (not (string? (bot :avatar)))
           (do (timbre/warnf "User %s tried to change bot to not have an avatar"
                             user-id (bot :webhook-url))
               {:reply! {:braid/error "Bot needs an avatar image"}})

           :else
           (let [updating (select-keys bot [:name :avatar :webhook-url
                                            :notify-all-messages?
                                            :event-webhook-url])
                 updating (if (string/blank? (bot :event-webhook-url))
                            (dissoc updating :event-webhook-url)
                            updating)
                 updating (reduce
                            (fn [m [k v]]
                              (if (= v (get orig-bot k))
                                m
                                (assoc m (keyword "bot" (name k)) v)))
                            {:db/id [:bot/id (bot :id)]}
                            updating)
                 [updated] (db/run-txns! (bot/update-bot-txn updating))]
             (timbre/debugf "Updated bot: %s" updated)
             {:reply! {:braid/ok updated}
              :group-broadcast! [(orig-bot :group-id)
                                 [:braid.client/edit-bot
                                  [(orig-bot :group-id) (bot->display updated)]]]})))))

   :braid.server/retract-bot
   (fn [{:as ev-msg bot-id :?data user-id :user-id}]
     (let [bot (bot/bot-by-id bot-id)]
       (when (group/user-is-group-admin? user-id (bot :group-id))
         {:db-run-txns! (bot/retract-bot-txn bot-id)
          :reply! {:braid/ok true}
          :group-broadcast! [(bot :group-id)
                             [:braid.client/retract-bot [(bot :group-id) bot-id]]]})))

   :braid.server/get-bot-info
   (fn [{:as ev-msg :keys [?data user-id]}]
     (let [bot (bot/bot-by-id ?data)]
       (when (and bot (group/user-is-group-admin? user-id (bot :group-id)) ?reply-fn)
         {:reply! {:braid/ok bot}})))})

(defn group-change-broadcast!
  [group-id info]
  (doseq [bot (db/bots-for-event group-id)]
    (bots/send-event-notification bot info)))
