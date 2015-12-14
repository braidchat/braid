(ns chat.server.search
  (:require [datomic.api :as d]
            [chat.server.db :as db]
            ))


(defn search-threads-as
  [user-id text]
  ; TODO: pagination?
  ; TODO: consistent order for results
  (->> (d/q '[:find [?t-id ...]
              :in $ ?txt
              :where
              [(fulltext $ :message/content ?txt) [[?m]]]
              [?m :message/thread ?t]
              [?t :thread/id ?t-id]]
            (d/db db/*conn*) text)
       (into #{} (filter (partial db/user-can-see-thread? user-id)))))
