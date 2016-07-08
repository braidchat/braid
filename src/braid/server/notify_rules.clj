(ns braid.server.notify-rules
  (:require [clojure.set :as set]
            [braid.common.schema :refer [check-new-message! check-rules!]]
            [braid.server.db :as db]))

(defn tag->group
  [tag-id]
  (db/tag-group-id tag-id))

(defn thread->tags
  [thread-id]
  (:tag-ids (db/thread-by-id thread-id)))

(defn thread->groups [thread-id]
  (into #{} (map tag->group) (thread->tags thread-id)))

(defn notify?
  [user-id rules new-message]
  (check-rules! rules)
  (check-new-message! new-message)
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
