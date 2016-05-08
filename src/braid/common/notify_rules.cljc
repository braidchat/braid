(ns braid.common.notify-rules
  (:require [clojure.set :as set]
            [schema.core :as s :include-macros true]
            [braid.common.schema :refer [Rules NewMessage]]
            #?@(:cljs ([chat.client.store :as store])
                :clj ([chat.server.db :as db]))))

(def rules-valid? (s/validator Rules))
(def new-message-valid? (s/validator NewMessage))

(defn tag->group
  [tag-id]
  #? (:clj (db/with-conn (db/tag-group-id tag-id))
      :cljs (store/group-for-tag tag-id)))

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
        (let [groups (into #{} (map tag->group) (new-message :mentioned-tag-ids))]
          (or ; ...or by anything in this group
              (seq (set/intersection groups any))
              ; ...or by a mention...
              (and (seq mention) ((set (new-message :mentioned-user-ids)) user-id)
                (or (mention :any) ; in any group
                    ; or this group
                    (seq (set/intersection
                           groups
                           mention)))))))))
