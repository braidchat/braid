(ns braid.server.db.user
  (:require [datomic.api :as d]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [crypto.password.scrypt :as password]
            [clavatar.core :refer [gravatar]]
            [braid.server.db.common :refer :all]
            [braid.server.quests.db :refer [activate-first-quests!]]
            [braid.common.util :refer [slugify]]))

(defn email-taken?
  [conn email]
  (some? (d/entity (d/db conn) [:user/email email])))


(defn generate-nickname-from-email
  "Generates a nickname from an email string"
  [email]
  (-> email
      (string/split #"@")
      first
      slugify))

(defn create-user!
  "given an id and email, creates and returns a user;
  the nickname and avatar are set based on the email;
  the id, email, and resulting nickname must be unique"
  [conn {:keys [id email]}]
  (let [user (->>
               {:user/id id
                :user/email email
                :user/nickname (generate-nickname-from-email email)
                :user/avatar (gravatar email
                               :rating :g
                               :default :identicon)}
               (create-entity! conn)
               db->user)]
    (activate-first-quests! conn id)
    user))

(defn create-bot-user!
  [conn {:keys [id]}]
  (->> {:user/id id
        :user/is-bot? true}
       (create-entity! conn)
       :user/id))

(defn nickname-taken?
  [conn nickname]
  (some? (d/entity (d/db conn) [:user/nickname nickname])))

(defn set-nickname!
  "Set the user's nickname"
  [conn user-id nickname]
  @(d/transact conn [[:db/add [:user/id user-id] :user/nickname (slugify nickname)]]))

(defn set-user-avatar!
  [conn user-id avatar]
  @(d/transact conn [[:db/add [:user/id user-id] :user/avatar avatar]]))

(defn set-user-password!
  [conn user-id password]
  @(d/transact conn [[:db/add [:user/id user-id]
                      :user/password-token (password/encrypt password)]]))

(defn authenticate-user
  "returns user-id if email and password are correct"
  [conn email password]
  (->> (let [[user-id password-token]
             (d/q '[:find [?id ?password-token]
                    :in $ ?email
                    :where
                    [?e :user/id ?id]
                    [?e :user/email ?stored-email]
                    [(.toLowerCase ^String ?stored-email) ?email]
                    [?e :user/password-token ?password-token]]
                  (d/db conn)
                  (.toLowerCase email))]
         (when (and user-id (password/check password password-token))
           user-id))))

(defn user-by-id
  [conn id]
  (some-> (d/pull (d/db conn) user-pull-pattern [:user/id id])
          db->user))

(defn user-id-exists?
  [conn id]
  (some? (d/entity (d/db conn) [:user/id id])))

(defn user-with-email
  "get the user with the given email address or nil if no such user registered"
  [conn email]
  (some-> (d/pull (d/db conn) user-pull-pattern [:user/email email])
          db->user))

(defn user-email
  [conn user-id]
  (:user/email (d/pull (d/db conn) [:user/email] [:user/id user-id])))


(defn user-get-preferences
  [conn user-id]
  (->> (d/pull (d/db conn)
               [{:user/preferences [:user.preference/key :user.preference/value]}]
               [:user/id user-id])
       :user/preferences
       (into {}
             (comp (map (juxt :user.preference/key :user.preference/value))
                   (map (fn [[k v]] [k (edn/read-string v)]))))))

(defn user-get-preference
  [conn user-id pref]
  (some-> (d/q '[:find ?val .
                 :in $ ?user-id ?key
                 :where
                 [?u :user/id ?user-id]
                 [?u :user/preferences ?p]
                 [?p :user.preference/key ?key]
                 [?p :user.preference/value ?val]]
               (d/db conn) user-id pref)
          edn/read-string))

(defn user-preference-is-set?
  "If the preference with the given key has been set for the user, return the
  entity id, else nil"
  [conn user-id pref]
  (d/q '[:find ?p .
         :in $ ?user-id ?key
         :where
         [?u :user/id ?user-id]
         [?u :user/preferences ?p]
         [?p :user.preference/key ?key]]
       (d/db conn) user-id pref))

(defn user-set-preference!
  "Set a key to a value for the user's preferences.  This will throw if
  permissions are changed in between reading & setting"
  [conn user-id k v]
  (if-let [e (user-preference-is-set? conn user-id k)]
    @(d/transact conn [[:db/add e :user.preference/value (pr-str v)]])
    @(d/transact conn [{:user.preference/key k
                        :user.preference/value (pr-str v)
                        :user/_preferences [:user/id user-id]
                        :db/id #db/id [:entities]}])))

(defn user-search-preferences
  "Find the ids of users that have the a given value for a given key set in
  their preferences"
  [conn k v]
  (d/q '[:find [?user-id ...]
         :in $ ?k ?v
         :where
         [?u :user/id ?user-id]
         [?u :user/preferences ?pref]
         [?pref :user.preference/key ?k]
         [?pref :user.preference/value ?v]]
       (d/db conn)
       k (pr-str v)))

(defn users-for-user
  "Get all users visible to given user"
  [conn user-id]
  (->> (d/q '[:find (pull ?e pull-pattern)
              :in $ ?user-id pull-pattern
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?g :group/user ?e]]
            (d/db conn)
            user-id
            user-pull-pattern)
       (map (comp db->user first))
       set))

(defn user-visible-to-user?
  "Are the two user ids users that can see each other? i.e. do they have at least one group in common"
  [conn user1-id user2-id]
  (-> (d/q '[:find ?g
             :in $ ?u1-id ?u2-id
             :where
             [?u1 :user/id ?u1-id]
             [?u2 :user/id ?u2-id]
             [?g :group/user ?u1]
             [?g :group/user ?u2]]
           (d/db conn) user1-id user2-id)
      seq boolean))

