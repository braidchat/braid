(ns braid.server.db.bot
  (:require [datomic.api :as d]
            [braid.server.db.common :refer [create-entity! bot-pull-pattern db->bot]]
            [braid.server.db.user :as user]
            [braid.server.crypto :as crypto :refer [random-nonce]]))

(defn create-bot!
  [conn {:keys [id name avatar webhook-url group-id]}]
  ; TODO: enforce name is unique in that group?
  (let [fake-user (user/create-bot-user! conn {:id (d/squuid)})]
    (->> {:bot/id id
          :bot/name name
          :bot/avatar avatar
          :bot/webhook-url webhook-url
          :bot/token (random-nonce 30)
          :bot/group [:group/id group-id]
          :bot/user [:user/id fake-user]}
         (create-entity! conn)
         db->bot)))

(defn bots-in-group
  [conn group-id]
  (->> (d/q '[:find [(pull ?b pull-pattern) ...]
              :in $ pull-pattern ?group-id
              :where
              [?g :group/id ?group-id]
              [?b :bot/group ?g]]
            (d/db conn) bot-pull-pattern group-id)
       (into #{} (map db->bot))))

(defn bot-by-name-in-group
  [conn name group-id]
  (some-> (d/q '[:find (pull ?b pull-pattern) .
                 :in $ pull-pattern ?group-id ?name
                 :where
                 [?g :group/id ?group-id]
                 [?b :bot/group ?g]
                 [?b :bot/name ?name]]
               (d/db conn) bot-pull-pattern group-id name)
          db->bot))

(defn bot-auth?
  "Check if token is correct for the bot with the given id"
  [conn bot-id token]
  (some-> (d/pull (d/db conn) [:bot/token] [:bot/id bot-id])
          :bot/token
          (crypto/constant-comp token)))

(defn bot-by-id
  [conn bot-id]
  (db->bot (d/pull (d/db conn) bot-pull-pattern [:bot/id bot-id])))

(defn bot-watch-thread!
  [conn bot-id thread-id]
  ; need to verify that thread is in bot's group
  @(d/transact conn
     [[:db/add [:bot/id bot-id]
       :bot/watched [:thread/id thread-id]]]))

(defn bots-watching-thread
  [conn thread-id]
  (some->> (d/q '[:find [(pull ?b pull-pattern) ...]
                  :in $ pull-pattern ?thread-id
                  :where
                  [?t :thread/id ?thread-id]
                  [?b :bot/watched ?t]
                  [?t :thread/group ?g]
                  [?b :bot/group ?g]]
                (d/db conn) bot-pull-pattern thread-id)
       (into #{} (map db->bot))))
