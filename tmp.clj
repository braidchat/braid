

(let [tags (->> (with-conn *conn* (fetch-tags-for-user #uuid "55b65d64-0c21-43c4-894f-5fd3c2b5f827"))
                (filter (fn [t] (= (t :threads-count) 1)))
                (map :id)
                (map (fn [id] [:db.fn/retractEntity [:tag/id id]])))]
  (with-conn (d/transact *conn* tags)))

(let [user-id #uuid "55b65d64-0c21-43c4-894f-5fd3c2b5f827"
      group-id #uuid "568d99c5-8ecd-4bd2-b8d2-35242f03c741"
      tags (->> (with-conn *conn* (d/q '[:find [?tag-id ...]
                               :in $ ?user-id ?group-id
                               :where
                               [?user :user/id ?user-id]
                               [?user :user/subscribed-tag ?tag]
                               [?tag :tag/id ?tag-id]
                               [?tag :tag/group ?g]
                               [?tag :tag/name ?n]
                               [?g :group/id ?group-id]
                               ]
                             (d/db *conn*)
                             user-id
                             group-id))
                (map (fn [id] [:db.fn/retractEntity [:tag/id id]])))]
  #_tags
  (with-conn (d/transact *conn* tags)))

(let [user-id #uuid "55b65d64-0c21-43c4-894f-5fd3c2b5f827"]
  (with-conn *conn* (get-groups-for-user user-id)))


(defn rename-clojurians-to-braid []
  (let [group-id #uuid "568d99c5-8ecd-4bd2-b8d2-35242f03c741"]

    (with-conn
      (d/transact *conn* [[:db/add [:group/id group-id]
                           :group/name "Braid"]]))

    ))
