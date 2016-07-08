(ns braid.server.message-format
  (:require [clojure.string :as string]
            [braid.server.db :as db]))

(defn str->uuid
  [s]
  (java.util.UUID/fromString s))

(defn parse-tags-and-mentions
  [user-id content]
  (let [id->nick (into {} (map (juxt :id :nickname)) (db/users-for-user user-id))
        id->tag (into {} (map (juxt :id :name)) (db/tags-for-user user-id))
        uuid-re #"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
        tag-re (re-pattern (str "#" uuid-re))
        mention-re (re-pattern (str "@" uuid-re))]
    (-> content
      (string/replace tag-re
                      (comp (partial str "#") id->tag str->uuid second))
      (string/replace
        mention-re
        (comp (partial str "@") id->nick str->uuid second)))))

