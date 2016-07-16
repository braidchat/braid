(ns braid.test.server.test-utils
  (:require [datomic.api :as d]
            [braid.server.db :as db]))

(defn- db->message
  [e]
  {:id (:message/id e)
   :content (:message/content e)
   :user-id (:user/id (:message/user e))
   :thread-id (:thread/id (:message/thread e))
   :created-at (:message/created-at e)})

(defn fetch-messages
  "Helper function to fetch all messages"
  []
  (->> (d/q '[:find (pull ?e [:message/id
                              :message/content
                              :message/created-at
                              {:message/user [:user/id]}
                              {:message/thread [:thread/id]}])
              :where [?e :message/id]]
            (d/db db/conn))
       (map (comp db->message first))))
