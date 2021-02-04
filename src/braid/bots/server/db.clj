(ns braid.bots.server.db
  (:require
    [braid.core.server.db :as db]
    [braid.chat.db.user :as user]
    [braid.chat.db.group :as group]
    [braid.lib.crypto :as crypto :refer [random-nonce]]
    [clojure.set :refer [rename-keys]]
    [datomic.api :as d]))

(def schema
  [;; need to add some extra info to user
   ;; [TODO] are we actually using this? seems like the front-end is
   ;; just checking if the user id is the same as a bot's user-id
   {:db/ident :user/is-bot?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   ;; bot stuff
   {:db/ident :bot/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :bot/token
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :bot/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :bot/avatar
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :bot/webhook-url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :bot/event-webhook-url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :bot/group
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :bot/user
    :db/doc "Fake user bot posts under"
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :bot/watched
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :bot/notify-all-messages?
    :db/doc "Indicates that this bot should recieve all visible messages in its group"
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}])

(def bot-pull-pattern
  [:bot/id
   :bot/name
   :bot/token
   :bot/avatar
   :bot/webhook-url
   :bot/event-webhook-url
   :bot/notify-all-messages?
   {:bot/group [:group/id]}
   {:bot/user [:user/id]}])

(defn db->bot [e]
  {:id (:bot/id e)
   :user-id (get-in e [:bot/user :user/id])
   :group-id (get-in e [:bot/group :group/id])
   :name (:bot/name e)
   :avatar (:bot/avatar e)
   :webhook-url (:bot/webhook-url e)
   :event-webhook-url (:bot/event-webhook-url e)
   :notify-all-messages? (:bot/notify-all-messages? e)
   :token (:bot/token e)})

(def bot-display-pull-pattern
  "Like bot-pull-pattern but for the publicy-visible bot attributes"
  [:bot/id
   :bot/name
   :bot/avatar
   {:bot/user [:user/id]}])

(defn db->bot-display
  "Like db->bot but for the publicly-visible bot attributes"
  [e]
  {:id (:bot/id e)
   :user-id (get-in e [:bot/user :user/id])
   :nickname (:bot/name e)
   :avatar (:bot/avatar e)})

(defn bot->display
  "Convert a private bot to public "
  [b]
  (-> b
      (rename-keys {:name :nickname})
      (select-keys (keys (db->bot-display nil)))))

(defn bots-in-group
  [group-id]
  (->> (d/q '[:find [(pull ?b pull-pattern) ...]
              :in $ pull-pattern ?group-id
              :where
              [?g :group/id ?group-id]
              [?b :bot/group ?g]]
            (db/db) bot-pull-pattern group-id)
       (into #{} (map db->bot))))

(defn bots-for-user-groups
  "Get the bots for the user's groups"
  [user-id]
  (into {}
        (comp (map :id)
              (map (juxt identity
                         (comp set (partial map bot->display) bots-in-group))))
        (group/user-groups user-id)))

(defn bot-by-name-in-group
  [name group-id]
  (some-> (d/q '[:find (pull ?b pull-pattern) .
                 :in $ pull-pattern ?group-id ?name
                 :where
                 [?g :group/id ?group-id]
                 [?b :bot/group ?g]
                 [?b :bot/name ?name]]
               (db/db) bot-pull-pattern group-id name)
          db->bot))

(defn bot-auth?
  "Check if token is correct for the bot with the given id"
  [bot-id token]
  (some-> (d/pull (db/db) [:bot/token] [:bot/id bot-id])
          :bot/token
          (crypto/constant-comp token)))

(defn bot-by-id
  [bot-id]
  (db->bot (d/pull (db/db) bot-pull-pattern [:bot/id bot-id])))

(defn bots-watching-thread
  [thread-id]
  (some->> (d/q '[:find [(pull ?b pull-pattern) ...]
                  :in $ pull-pattern ?thread-id
                  :where
                  [?t :thread/id ?thread-id]
                  [?b :bot/watched ?t]]
                (db/db) bot-pull-pattern thread-id)
       (into #{} (map db->bot))))

(defn bots-for-event
  "Get all bots in the given group that want to be notified of events"
  [group-id]
  (->> (d/q '[:find [(pull ?b pull-pattern) ...]
              :in $ pull-pattern ?group-id
              :where
              [?b :bot/event-webhook-url]
              [?g :group/id ?group-id]
              [?b :bot/group ?g]]
            (db/db) bot-pull-pattern group-id)
       (into #{} (map db->bot))))

(defn bots-for-message
  "Get all bots in the group of the given thread that are subscribed to all messages"
  [thread-id]
  (->> (d/q '[:find [(pull ?b pull-pattern) ...]
              :in $ pull-pattern ?thread-id
              :where
              [?t :thread/id ?thread-id]
              [?t :thread/group ?g]
              [?b :bot/group ?g]
              [?b :bot/notify-all-messages? true]]
            (db/db) bot-pull-pattern thread-id)
       (into #{} (map db->bot))))

;; Transactions

(defn create-bot-txn
  [{:keys [id name avatar webhook-url event-webhook-url group-id
           notify-all-messages?]
    :or {notify-all-messages? false}}]
  ; TODO: enforce name is unique in that group?
  (let [fake-user-id (d/tempid :entities)
        bot-id (d/tempid :entities)]
    [{:db/id fake-user-id
      :user/id (d/squuid)
      :user/is-bot? true}
     (with-meta
       (merge
         {:db/id bot-id
          :bot/id id
          :bot/name name
          :bot/avatar avatar
          :bot/webhook-url webhook-url
          :bot/token (random-nonce 30)
          :bot/group [:group/id group-id]
          :bot/user fake-user-id
          :bot/notify-all-messages? notify-all-messages?}
         (when-let [ewu event-webhook-url]
           {:bot/event-webhook-url event-webhook-url}))
       {:braid.core.server.db/return
        (fn [{:keys [db-after tempids]}]
          (->> (d/resolve-tempid db-after tempids bot-id)
               (d/entity db-after)
               db->bot))})]))

(defn retract-bot-txn
  [bot-id]
  [[:db.fn/retractEntity [:bot/id bot-id]]])

(defn update-bot-txn
  [bot]
  [(with-meta
     bot
     {:braid.core.server.db/return
      (fn [{:keys [db-after]}]
        (-> (d/entity db-after (bot :db/id))
            db->bot))})])

(defn bot-watch-thread-txn
  [bot-id thread-id]
  [^{:braid.core.server.db/check
     (fn [{:keys [db-after]}]
       (db/assert
         (= (get-in (d/entity db-after [:bot/id bot-id]) [:bot/group :group/id])
            (get-in (d/entity db-after [:thread/id thread-id]) [:thread/group :group/id]))
         (format "Bot %s tried to watch thread not in its group %s" bot-id thread-id)))}
   [:db/add [:bot/id bot-id] :bot/watched [:thread/id thread-id]]])
