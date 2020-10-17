(ns braid.core.server.sync-helpers
  (:require
   [braid.core.hooks :as hooks]
   [braid.chat.db.group :as group]
   [braid.chat.db.tag :as tag]
   [braid.chat.db.thread :as thread]
   [braid.chat.db.user :as user]
   [braid.core.server.email-digest :as email]
   [braid.core.server.message-format :as message-format]
   [braid.core.server.notify-rules :as notify-rules]
   [braid.base.server.socket :refer [chsk-send! connected-uids]]
   [clojure.set :refer [difference intersection]]
   [taoensso.timbre :as timbre]))

(def anonymous-group-readers (atom {}))

(defn add-anonymous-reader
  [group-id client-id]
  (swap! anonymous-group-readers update group-id (fnil conj #{}) client-id))

(defn remove-anonymous-reader
  [client-id]
  (let [group (some (fn [[g ids]]
                      (and (contains? ids client-id)
                           g))
                    @anonymous-group-readers)]
    (swap! anonymous-group-readers update group disj client-id)))

(defn broadcast-thread
  "broadcasts thread to all users with the thread open, except those in ids-to-skip"
  [thread-id ids-to-skip]
  (let [user-ids (-> (difference
                       (intersection
                         (set (thread/users-with-thread-open thread-id))
                         (set (:any @connected-uids)))
                       (set ids-to-skip)))
        thread (thread/thread-by-id thread-id)]
    (doseq [uid user-ids]
      (let [user-tags (tag/tag-ids-for-user uid)
            filtered-thread (update-in thread [:tag-ids]
                                       (partial into #{} (filter user-tags)))
            thread-with-last-opens (thread/thread-add-last-open-at
                                     filtered-thread uid)]
        (chsk-send! uid [:braid.client/thread thread-with-last-opens])))
    (doseq [anon-id (@anonymous-group-readers (thread :group-id))]
      (chsk-send! anon-id [:braid.client/thread thread]))))

(defonce group-change-broadcast-hooks
  (hooks/register! (atom []) [fn?]))

(defn broadcast-group-change
  "Broadcast group change to clients that are in the group"
  [group-id info]
  (let [ids-to-send-to (intersection
                         (set (:any @connected-uids))
                         (into #{} (map :id)
                               (group/group-users group-id)))]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info)))
  (doseq [anon-id (@anonymous-group-readers group-id)]
    (chsk-send! anon-id info))
  (doseq [hook @group-change-broadcast-hooks]
    (hook group-id info)))

;; TODO: when using clojure.spec, use spec to validate this
(defn user-can-message? [user-id ?data]
  (every?
      true?
      (concat
        [(or (boolean (thread/user-can-see-thread? user-id (?data :thread-id)))
             (do (timbre/warnf
                   "User %s attempted to add message to disallowed thread %s"
                   user-id (?data :thread-id))
                 false))
         (or (boolean (if-let [cur-group (thread/thread-group-id (?data :thread-id))]
                        (= (?data :group-id) cur-group)
                        true)))
         (group/user-in-group? user-id (?data :group-id))]
        (map
          (fn [tag-id]
            (and
              (or (boolean (= (?data :group-id) (tag/tag-group-id tag-id)))
                  (do
                    (timbre/warnf
                      "User %s attempted to add a tag %s from a different group"
                      user-id tag-id)
                    false))
              (or (boolean (tag/user-in-tag-group? user-id tag-id))
                  (do
                    (timbre/warnf "User %s attempted to add a disallowed tag %s"
                                  user-id tag-id)
                    false))))
          (?data :mentioned-tag-ids))
        (map
          (fn [mentioned-id]
            (and
              (or (boolean (group/user-in-group? user-id (?data :group-id)))
                  (do (timbre/warnf
                        "User %s attempted to mention disallowed user %s"
                        user-id mentioned-id)
                      false))
              (or (boolean (user/user-visible-to-user? user-id mentioned-id))
                  (do (timbre/warnf
                        "User %s attempted to mention disallowed user %s"
                        user-id mentioned-id)
                    false))))
          (?data :mentioned-user-ids)))))

(defn notify-users [new-message]
  (let [thread-id (new-message :thread-id)
        subscribed-user-ids (->>
                              (thread/users-subscribed-to-thread thread-id)
                              (remove (partial = (:user-id new-message))))
        online? (intersection
                  (set subscribed-user-ids)
                  (set (:any @connected-uids)))
        parse-tags-and-mentions (message-format/make-tags-and-mentions-parser (new-message :group-id))]
    (doseq [uid subscribed-user-ids]
      (when-let [rules (user/user-get-preference uid :notification-rules)]
        (when (notify-rules/notify? uid rules new-message)
          (if (online? uid)
            (let [msg (update new-message :content
                              parse-tags-and-mentions)]
              (chsk-send! uid [:braid.client/notify-message msg]))
            (future
              (let [update-msgs
                    (partial
                      map
                      (fn [m] (update m :content
                                      parse-tags-and-mentions)))]
                (-> (email/create-message
                      [(-> (thread/thread-by-id thread-id)
                           (update :messages update-msgs))])
                    (assoc :subject "Notification from Braid")
                    (->> (email/send-message (user/user-email uid))))))))))))
