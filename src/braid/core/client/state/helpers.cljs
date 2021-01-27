(ns braid.core.client.state.helpers
  (:require
   [braid.core.client.schema :as schema]
   [braid.lib.misc :as misc]
   [clojure.set :as set]))

(def key-by misc/key-by)

(def key-by-id (partial key-by :id))

(defn order-groups
  "Helper function to impose an order on groups.
  This is a seperate function (instead of inline in :ordered-groups because the
  index route needs to be able to call this to find the first group"
  [groups group-order]
  (if (nil? group-order)
    groups
    (let [ordered? (comp boolean (set group-order) :id)
          {ord true unord false} (group-by ordered? groups)
          by-id (group-by :id groups)]
      (concat
        (remove nil? (map (comp first by-id) group-order))
        unord))))

; ALL HELPERS BELOW SHOULD TAKE STATE AS FIRST ARG

; GETTERS

(defn get-user-preferences [state]
  (get state :preferences))

(defn current-user-id [state]
  (get-in state [:session :user-id]))

(defn ordered-groups [state]
  (order-groups (vals (state :groups)) (get-in state [:preferences :groups-order])))

; SETTERS

; page

(defn set-group-and-page [state group-id page-id]
  (assoc state
    :open-group-id group-id
    :page page-id))

(defn set-page-error [state bool]
  (assoc-in state [:page :error?] bool))

; user

(defn set-preferences [state prefs]
  (update-in state [:preferences] merge prefs))

; users

(defn update-user-avatar [state {:keys [group-id user-id avatar-url]}]
  (assoc-in state [:groups group-id :users user-id :avatar] avatar-url))

; threads and messages

(defn update-thread-last-open-at [state thread-id]
  (if-let [thread (get-in state [:threads thread-id])]
    (let [latest-message (->> (thread :messages)
                              (map :created-at)
                              (reduce max (js/Date. 0)))
          new-last-open (js/Date. (inc (.getTime latest-message)))]
      (update-in state [:threads thread-id :last-open-at] (constantly new-last-open)))
    state))

(def blank-thread
  {:id nil
   :group-id nil
   :tag-ids []
   :mentioned-ids []
   :new-message ""
   :messages []})

(defn add-to-open-threads [state thread-id]
  (update-in state [:user :open-thread-ids] conj thread-id))

(defn create-thread [state partial-thread]
  {:pre [(contains? partial-thread :id)
         (contains? partial-thread :group-id)]}
  (let [thread (merge
                 blank-thread
                 partial-thread)]
    (-> state
        (assoc-in [:threads (:id thread)] thread)
        (update-in [:group-threads (:group-id thread)] #(conj (set %) (:id thread))))))

(defn maybe-create-thread [state partial-thread]
  (if-not (get-in state [:threads (:id partial-thread)])
    (create-thread state partial-thread)
    state))

(defn add-message [state message]
  (-> state
      (maybe-create-thread {:id (message :thread-id)
                            :group-id (message :group-id)})
      (add-to-open-threads (message :thread-id))
      (update-in [:threads (message :thread-id) :messages] conj message)
      (update-thread-last-open-at (message :thread-id))
      (update-in [:threads (message :thread-id) :tag-ids]
                 (partial set/union (set (message :mentioned-tag-ids))))
      (update-in [:threads (message :thread-id) :mentioned-ids]
                 (partial set/union (set (message :mentioned-user-ids))))))

; tags

(defn subscribe-to-tag [state tag-id]
  (update-in state [:user :subscribed-tag-ids] conj tag-id))

(defn add-tags [state tags]
  (update-in state [:tags] merge (key-by-id tags)))

; groups

(defn add-group [state group]
  (-> state
      (assoc-in [:groups (group :id)] group)))

(defn remove-group [state group-id]
  (let [group-threads (get-in state [:group-threads group-id])]
    (-> state
        (update-in [:threads] #(apply dissoc % group-threads))
        (update-in [:group-threads] (misc/flip dissoc group-id))
        (update-in [:groups] (misc/flip dissoc group-id)))))
