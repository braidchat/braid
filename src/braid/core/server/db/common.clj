(ns braid.core.server.db.common
  (:require
    [clojure.edn :as edn]
    [clojure.set :refer [rename-keys]]
    [braid.core.server.db :as db]
    [datomic.api :as d]))

(defn create-entity-txn
  "create entity with attrs, return (after-fn entity)"
  [attrs after-fn]
  (let [new-id (d/tempid :entities)]
    [(with-meta
       (assoc attrs :db/id new-id)
       {:braid.core.server.db/return
        (fn [{:keys [db-after tempids]}]
          (->> (d/resolve-tempid db-after tempids new-id)
               (d/entity db-after)
               (after-fn)))})]))

(def user-pull-pattern
  '[:user/id
    :user/nickname
    :user/avatar
    {:group/_user [:group/id]}])

(defn user-joined-at
  [user-id group-id]
  (->>
   (d/q '[:find ?inst
          :in $ ?user-id ?group-id
          :where
          [?u :user/id ?user-id]
          [?g :group/id ?group-id]
          [?g :group/user ?t ?tx true]
          [?tx :db/txInstant ?inst]]
        (d/history (db/db))
        user-id
        group-id)
   (reduce min)
   (first)))

(defn db->user
  [e]
  {:id (:user/id e)
   :nickname (:user/nickname e)
   :avatar (:user/avatar e)
   :email (:user/email e)
   ; TODO currently leaking all group-ids to the client
   :group-ids (map :group/id (:group/_user e))
   :joined-at (->> (:group/_user e)
                   (map :group/id)
                   (map (fn [group-id]
                          [group-id (user-joined-at (:user/id e) group-id)]))
                   (into {}))})

(def private-user-pull-pattern
  '[:user/id
    :user/nickname
    :user/avatar
    :user/email
    {:group/_user [:group/id]}])

(defn db->private-user
  [e]
  {:id (:user/id e)
   :nickname (:user/nickname e)
   :avatar (:user/avatar e)
   :email (:user/email e)
   :group-ids (map :group/id (:group/_user e))})

(def message-pull-pattern
  '[:message/id
    :message/content
    {:message/user [:user/id]}
    {:message/thread [:thread/id {:thread/group [:group/id]}]}
    :message/created-at])

(defn db->message
  [e]
  {:id (:message/id e)
   :content (:message/content e)
   :user-id (:user/id (:message/user e))
   :thread-id (:thread/id (:message/thread e))
   :group-id (get-in e [:message/thread :thread/group :group/id])
   :created-at (:message/created-at e)})

(defn db->invitation [e]
  {:id (:invite/id e)
   :inviter-id (get-in e [:invite/from :user/id])
   :inviter-email (get-in e [:invite/from :user/email])
   :inviter-nickname (get-in e [:invite/from :user/nickname])
   :invitee-email (:invite/to e)
   :group-id (get-in e [:invite/group :group/id])
   :group-name (get-in e [:invite/group :group/name])})

(defn db->tag
  [e]
  {:id (:tag/id e)
   :name (:tag/name e)
   :description (:tag/description e)
   :group-id (get-in e [:tag/group :group/id])
   :threads-count (get e :tag/threads-count 0)
   :subscribers-count (get e :tag/subscribers-count 0)})

(def thread-pull-pattern
  [:thread/id
   {:thread/tag [:tag/id]}
   {:thread/group [:group/id]}
   {:thread/mentioned [:user/id]}
   {:message/_thread [:message/id
                      :message/content
                      {:message/user [:user/id]}
                      :message/created-at]}])

(defn db->thread
  [thread]
  {:id (thread :thread/id)
   :group-id (get-in thread [:thread/group :group/id])
   :messages (map (fn [msg]
                    {:id (msg :message/id)
                     :content (msg :message/content)
                     :user-id (get-in msg [:message/user :user/id])
                     :created-at (msg :message/created-at)})
                  (thread :message/_thread))
   :tag-ids (into #{} (map :tag/id) (thread :thread/tag))
   :mentioned-ids (into #{} (map :user/id) (thread :thread/mentioned))})

(def group-pull-pattern
  [:group/id
   :group/slug
   :group/name
   :group/settings
   {:group/admins [:user/id]}
   {:group/user user-pull-pattern}])

(defn db->group [e]
  (let [settings (-> e (get :group/settings "{}") edn/read-string)]
    {:id (:group/id e)
     :name (:group/name e)
     :slug (:group/slug e)
     :admins (into #{} (map :user/id) (:group/admins e))
     :intro (settings :intro)
     :avatar (settings :avatar)
     :public? (get settings :public? false)
     :users (into {}
                  (comp (map db->user)
                        (map #(dissoc % :group-ids :email))
                        (map (juxt :id identity)))
                  (:group/user e))
     :users-count (count (:group/user e))}))
