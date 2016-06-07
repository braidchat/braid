(ns braid.server.db.bot
  (:require [datomic.api :as d]
            [braid.server.db.common :refer [create-entity! bot-pull-pattern db->bot]]
            [braid.server.util :refer [random-nonce]]))

(defn create-bot!
  [conn {:keys [id name avatar webhook-url group-id]}]
  ; TODO: enforce name is unique in that group?
  (->> {:bot/id id
        :bot/name name
        :bot/avatar avatar
        :bot/webhook-url webhook-url
        :bot/token (random-nonce 20)
        :bot/group [:group/id group-id]}
       (create-entity! conn)
       db->bot))

(defn bots-in-group
  [conn group-id]
  (->> (d/q '[:find [(pull ?b pull-pattern) ...]
              :in $ pull-pattern ?group-id
              :where
              [?g :group/id ?group-id]
              [?b :bot/group ?g]]
            (d/db conn) bot-pull-pattern group-id)
       (into #{} (map db->bot))))
