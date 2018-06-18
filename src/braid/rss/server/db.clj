(ns braid.rss.server.db
  (:require
   [braid.core.server.db :as db]
   [clojure.string :as string]
   [datomic.api :as d]
   [datomic.db]))

(def schema
  [{:db/ident :rss/id
    :db/valueType :db.type/uuid
    :db/doc "Identifer of the RSS feed"
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :rss/feed-url
    :db/valueType :db.type/string
    :db/doc "URL of the feed to fetch"
    :db/cardinality :db.cardinality/one
    :db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :rss/last-fetched
    :db/valueType :db.type/string
    :db/doc "Unique identifier of the last fetched feed item. Either the item <guid> or a hash of the content."
    :db/cardinality :db.cardinality/one
    :db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :rss/group
    :db/valueType :db.type/ref
    :db/doc "The group the feed entries will be posted to"
    :db/cardinality :db.cardinality/one
    :db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :rss/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user which the items will be posted as"
    :db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db}
   {:db/ident :rss/tags
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Tags which will be applied to feed items"
    :db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db}])

(def rss-pull-pattern
  [:rss/id
   :rss/feed-url
   :rss/last-fetched
   {:rss/group [:group/id]}
   {:rss/user [:user/id]}
   {:rss/tags [:tag/id]}])

(defn db->rss
  [{:rss/keys [id feed-url last-fetched group user tags]}]
  {:id id
   :feed-url feed-url
   :last-fetched last-fetched
   :group-id (:group/id group)
   :user-id (:user/id user)
   :tag-ids (map :tag/id tags)})

(defn group-feeds
  [group-id]
  (->> (d/q '[:find [(pull ?rss pull-pattern) ...]
              :in $ ?group-id pull-pattern
              :where
              [?rss :rss/group ?g]
              [?g :group/id ?group-id]]
            (db/db) group-id rss-pull-pattern)
       (map db->rss)))

(defn all-feeds
  []
  (->> (d/q '[:find [(pull ?rss pull-pattern) ...]
              :in $ pull-pattern
              :where
             [?rss :rss/id]]
           (db/db) rss-pull-pattern)
       (map db->rss)))

(defn feed-group-id
  [feed-id]
  (-> (d/pull (db/db) [{:rss/group [:group/id]}] [:rss/id feed-id])
      (get-in [:rss/group :group/id])))

(defn add-rss-feed-txn
  [{:keys [id feed-url group-id user-id tag-ids]}]
  (let [ent-id (d/tempid :entities)]
    (into [{:db/id ent-id
            :rss/id id
            :rss/feed-url feed-url
            :rss/group [:group/id group-id]
            :rss/user [:user/id user-id]}]
          (map (fn [tag-id] [:db/add ent-id :rss/tags [:tag/id tag-id]])
               tag-ids))))

(defn update-last-fetched-txn
  [feed-id new-last-fetched]
  [[:db/add [:rss/id feed-id] :rss/last-fetched new-last-fetched]])

(defn remove-feed-txn
  [feed-id]
  [[:db.fn/retractEntity [:rss/id feed-id]]])
