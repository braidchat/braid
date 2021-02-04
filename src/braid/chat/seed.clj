(ns braid.chat.seed
  (:require
    [braid.lib.uuid :as uuid]
    [braid.chat.db.group :as group]
    [braid.chat.db.tag :as tag]
    [braid.chat.db.user :as user]
    [braid.base.server.cqrs :as cqrs]
    [braid.core.server.db :as db]))

(def groups
  [{:group/id (uuid/squuid)
    :group/slug "braid"
    :group/name "Braid"}
   {:group/id (uuid/squuid)
    :group/slug "chat"
    :group/name "Chat"}])

(def users
  [{:user/id (uuid/squuid)
    :user/email "foo@example.com"
    :user/password "foofoofoo"}
   {:user/id (uuid/squuid)
    :user/email "bar@example.com"
    :user/password "barbarbar"}])

(def tags
  [{:tag/id (uuid/squuid)
    :tag/group-id (get-in groups [0 :group/id])
    :tag/name "braid"}
   {:tag/id (uuid/squuid)
    :tag/group-id (get-in groups [1 :group/id])
    :tag/name "watercooler"}
   {:tag/id (uuid/squuid)
    :tag/group-id (get-in groups [1 :group/id])
    :tag/name "abcde"}])

(def threads
  [{:thread/id (uuid/squuid)
    :thread/user-id (get-in users [0 :user/id])
    :thread/group-id (get-in groups [0 :group/id])}
   {:thread/id (uuid/squuid)
    :thread/group-id (get-in groups [1 :group/id])
    :thread/user-id (get-in users [1 :user/id])}])

(def messages
  [;; thread 0

   {:id (uuid/squuid)
    :thread-id (get-in threads [0 :thread/id])
    :user-id (get-in users [0 :user/id])
    :created-at (java.util.Date.)
    :content "Hello?"
    :mentioned-user-ids []
    :mentioned-tag-ids [(get-in tags [0 :tag/id])]}

   {:id (uuid/squuid)
    :thread-id (get-in threads [0 :thread/id])
    :user-id (get-in users [1 :user/id])
    :created-at (java.util.Date.)
    :content "Hi!"
    :mentioned-user-ids []
    :mentioned-tag-ids []}

   {:id (uuid/squuid)
    :thread-id (get-in threads [0 :thread/id])
    :user-id (get-in users [0 :user/id])
    :created-at (java.util.Date.)
    :content "Oh, great, someone else is here."
    :mentioned-user-ids []
    :mentioned-tag-ids []}

   {:id (uuid/squuid)
    :thread-id (get-in threads [0 :thread/id])
    :user-id (get-in users [1 :user/id])
    :created-at (java.util.Date.)
    :content "Yep!"
    :mentioned-user-ids []
    :mentioned-tag-ids []}

   ;; thread 1

   {:id (uuid/squuid)
    :thread-id (get-in threads [1 :thread/id])
    :user-id (get-in users [1 :user/id])
    :created-at (java.util.Date.)
    :content "Hello?"
    :mentioned-user-ids []
    :mentioned-tag-ids [(get-in tags [2 :tag/id])]}

   {:id (uuid/squuid)
    :thread-id (get-in threads [1 :thread/id])
    :user-id (get-in users [0 :user/id])
    :created-at (java.util.Date.)
    :content "Hi!"
    :mentioned-user-ids []
    :mentioned-tag-ids []}])

(defn seed! []
  (println "Create Groups")
  (doseq [group groups]
    (db/run-txns! (group/create-group-txn
                    {:id (:group/id group)
                     :slug (:group/slug group)
                     :name (:group/name group)})))

  (println "Create Users")
  (doseq [user users]
    (println ".")
    (db/run-txns! (user/create-user-txn
                    {:id (:user/id user)
                     :email (:user/email user)})))

  (println "Set user passwords")
  (doseq [user users]
    (println ".")
    (db/run-txns! (user/set-user-password-txn
                    (:user/id user)
                    (:user/password user))))

  (println "Add Users to Groups")
  (doseq [user users
          group groups]
    (println ".")
    (db/run-txns!
      (group/user-add-to-group-txn (user :user/id)
                                   (group :group/id))))

  (println "Make Users Admins")
  (db/run-txns! (group/user-make-group-admin-txn
                  (get-in users [0 :user/id])
                  (get-in groups [0 :group/id])))
  (db/run-txns! (group/user-make-group-admin-txn
                  (get-in users [1 :user/id])
                  (get-in groups [1 :group/id])))

  (println "Create Tags")
  (doseq [tag tags]
    (println ".")
    (db/run-txns! (tag/create-tag-txn
                    {:id (:tag/id tag)
                     :name (:tag/name tag)
                     :group-id (:tag/group-id tag)})))

  (println "Subscribe Users to Tags")
  (doseq [user users
          tag tags]
    (println ".")
    (db/run-txns!
      (tag/user-subscribe-to-tag-txn (:user/id user) (:tag/id tag))))

  (println "Create Threads")
  (doseq [thread threads]
    (println ".")
    (cqrs/dispatch :braid.chat/create-thread!
                   {:thread-id (:thread/id thread)
                    :user-id (:thread/user-id thread)
                    :group-id (:thread/group-id thread)}))

  (println "Create Messages")
  (doseq [message messages]
    (println ".")
    (cqrs/dispatch :braid.chat/create-message!
                   (-> message
                       (assoc :message-id (:id message))
                       (dissoc :id)))))


