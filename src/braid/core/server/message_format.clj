(ns braid.core.server.message-format
  (:require
    [clojure.string :as string]
    [braid.chat.db.group :as db.group]))

(defn str->uuid
  [s]
  (java.util.UUID/fromString s))

(defn make-tags-and-mentions-parser
  [group-id]
  (let [id->nick (into {} (map (juxt :id :nickname)) (db.group/group-users group-id))
        id->tag (into {} (map (juxt :id :name)) (db.group/group-tags group-id))]
    (fn [content]
      (let [uuid-re #"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
          tag-re (re-pattern (str "#" uuid-re))
          mention-re (re-pattern (str "@" uuid-re))]
      (-> content
          (string/replace tag-re
                          (comp (partial str "#") id->tag str->uuid second))
          (string/replace
            mention-re
            (comp (partial str "@") id->nick str->uuid second)))))))
