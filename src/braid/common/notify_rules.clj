(ns braid.common.notify-rules
  (:require [clojure.set :as set]
            [braid.common.schema :refer [new-message-valid? rules-valid?]]
            [chat.server.db :as db]))

(defn tag->group
  [tag-id]
  (db/with-conn (db/tag-group-id tag-id)))

(defn thread->tags
  [thread-id]
  (:tag-ids (db/with-conn (db/get-thread thread-id))))

(defn thread->groups [thread-id]
  (into #{} (map tag->group) (thread->tags thread-id)))

(defn notify?
  [user-id rules new-message]
  (assert (rules-valid? rules) (str "Malformed rules:" (pr-str rules)))
  (assert (new-message-valid? new-message)
    (str "Malformed new message: " (pr-str new-message)))
  (let [{:keys [tag mention any]
         :or {tag #{} mention #{} any #{}}}
        (->> (group-by first rules)
             (into {}
                   (map (fn [[k v]]
                          [k (into #{} (map second) v)]))))]
    ; notify if...
    (or (any :any) ; ...you want to be notified by anything in any group
        ; ...or by a tag that this thread has
        (seq (set/intersection
               (set (new-message :mentioned-tag-ids))
               tag))
        (let [groups (thread->groups (new-message :thread-id))]
          (or ; ...or by anything in this group
              (seq (set/intersection groups any))
              ; ...or by a mention...
              (and (seq mention) ((set (new-message :mentioned-user-ids)) user-id)
                (or (mention :any) ; in any group
                    ; or this group
                    (seq (set/intersection
                           groups
                           mention)))))))))
