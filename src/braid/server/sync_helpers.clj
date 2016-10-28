(ns braid.server.sync-helpers
  (:require
    [taoensso.timbre :as timbre :refer [debugf]]
    [clojure.set :refer [difference intersection]]
    [braid.server.db :as db]
    [braid.server.bots :as bots]
    [braid.server.notify-rules :as notify-rules]
    [braid.server.email-digest :as email]
    [braid.server.message-format :refer [parse-tags-and-mentions]]
    [braid.server.socket :refer [chsk-send! connected-uids]]))

(defn broadcast-thread
  "broadcasts thread to all users with the thread open, except those in ids-to-skip"
  [thread-id ids-to-skip]
  (let [user-ids (-> (difference
                       (intersection
                         (set (db/users-with-thread-open thread-id))
                         (set (:any @connected-uids)))
                       (set ids-to-skip)))
        thread (db/thread-by-id thread-id)]
    (doseq [uid user-ids]
      (let [user-tags (db/tag-ids-for-user uid)
            filtered-thread (update-in thread [:tag-ids]
                                       (partial into #{} (filter user-tags)))
            thread-with-last-opens (db/thread-add-last-open-at
                                     filtered-thread uid)]
        (chsk-send! uid [:braid.client/thread thread-with-last-opens])))))

(defn broadcast-user-change
  "Broadcast user info change to clients that can see this user"
  [user-id info]
  (let [ids-to-send-to (disj
                         (intersection
                           (set (:any @connected-uids))
                           (into
                             #{} (map :id)
                             (db/users-for-user user-id)))
                         user-id)]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info))))

(defn broadcast-group-change
  "Broadcast group change to clients that are in the group"
  [group-id info]
  (let [ids-to-send-to (intersection
                         (set (:any @connected-uids))
                         (into #{} (map :id)
                               (db/group-users group-id)))]
    (doseq [uid ids-to-send-to]
      (chsk-send! uid info))))

; TODO: when using clojure.spec, use spec to validate this
(defn user-can-message? [user-id ?data]
  ; TODO: also check that thread in group
  (every?
      true?
      (concat
        [(or (boolean (db/user-can-see-thread? user-id (?data :thread-id)))
             (do (timbre/warnf
                   "User %s attempted to add message to disallowed thread %s"
                   user-id (?data :thread-id))
                 false))
         (or (boolean (if-let [cur-group (db/thread-group-id (?data :thread-id))]
                        (= (?data :group-id) cur-group)
                        true)))]
        (map
          (fn [tag-id]
            (and
              (or (boolean (= (?data :group-id) (db/tag-group-id tag-id)))
                  (do
                    (timbre/warnf
                      "User %s attempted to add a tag %s from a different group"
                      user-id tag-id)
                    false))
              (or (boolean (db/user-in-tag-group? user-id tag-id))
                  (do
                    (timbre/warnf "User %s attempted to add a disallowed tag %s"
                                  user-id tag-id)
                    false))))
          (?data :mentioned-tag-ids))
        (map
          (fn [mentioned-id]
            (and
              (or (boolean (db/user-in-group? user-id (?data :group-id)))
                  (do (timbre/warnf
                        "User %s attempted to mention disallowed user %s"
                        user-id mentioned-id)
                      false))
              (or (boolean (db/user-visible-to-user? user-id mentioned-id))
                  (do (timbre/warnf
                        "User %s attempted to mention disallowed user %s"
                        user-id mentioned-id)
                    false))))
          (?data :mentioned-user-ids)))))

(defn notify-users [new-message]
  (let [subscribed-user-ids (->>
                              (db/users-subscribed-to-thread
                                (new-message :thread-id))
                              (remove (partial = (:user-id new-message))))
        online? (intersection
                  (set subscribed-user-ids)
                  (set (:any @connected-uids)))]
    (doseq [uid subscribed-user-ids]
      (when-let [rules (db/user-get-preference uid :notification-rules)]
        (when (notify-rules/notify? uid rules new-message)
          (let [msg (update new-message :content
                            (partial parse-tags-and-mentions uid))]
            (if (online? uid)
              (chsk-send! uid [:braid.client/notify-message msg])
              (let [update-msgs
                    (partial
                      map
                      (fn [m] (update m :content
                                      (partial parse-tags-and-mentions uid))))]
                (-> (email/create-message
                      [(-> (db/thread-by-id (msg :thread-id))
                           (update :messages update-msgs))])
                    (assoc :subject "Notification from Braid")
                    (->> (email/send-message (db/user-email uid))))))))))))

(defn notify-bots [new-message]
  ; Notify bots mentioned in the message
  (when-let [bot-name (second (re-find #"^/(\w+)\b" (:content new-message)))]
    (when-let [bot (db/bot-by-name-in-group bot-name (new-message :group-id))]
      (timbre/debugf "notifying bot %s" bot)
      (bots/send-notification bot new-message)))
  ; Notify bots subscribed to the thread
  (doseq [bot (db/bots-watching-thread (new-message :thread-id))]
    (timbre/debugf "notifying bot %s" bot)
    (bots/send-notification bot new-message)))
