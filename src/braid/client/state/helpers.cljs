(ns braid.client.state.helpers
  (:require [cljs-utils.core :refer [flip]]
            [clojure.set :as set]
            [cljs-uuid-utils.core :as uuid]))

(defn- key-by-id [coll]
  (into {} (map (juxt :id identity)) coll))

; ALL HELPERS BELOW SHOULD TAKE STATE AS FIRST ARG

; GETTERS

(defn group-for-tag
  "Get the group id for the tag with the given id"
  [state tag-id]
  (get-in state [:tags tag-id :group-id]))

(defn get-user-preferences [state]
  (get state :preferences))

(defn current-user-id [state]
  (get-in state [:session :user-id]))

(defn get-open-threads [state]
  (let [current-group-id (state :open-group-id)
        open-threads (-> (state :threads)
                         (select-keys (get-in state [:user :open-thread-ids]))
                         vals
                         (->> (filter (fn [thread]
                                        (or (empty? (thread :tag-ids))
                                            (contains?
                                              (into #{} (map (partial group-for-tag state) (thread :tag-ids)))
                                              current-group-id))))))]
    open-threads))

(defn get-open-group-id [state]
  (get state :open-group-id))

; SETTERS

; login

(defn set-login-state [state login-state]
  (assoc-in state [:login-state] login-state))

; window visibility and notifications

(defn set-window-visibility
    [state visible?]
    (if visible?
      (do
        (set! (.-title js/document) "Chat")
        (-> state
            (assoc-in [:notifications :unread-count] 0)
            (assoc-in [:notifications :window-visible?] visible?)))
      (assoc-in state [:notifications :window-visible?] visible?)))

(defn maybe-increment-unread [state]
  (if-not (get-in state [:notifications :window-visible?])
    (do
      ; TODO this should be done with a subscription
      ; TODO should store time when went away and recalculate unread-count instead of maintaing an unread-count in state
      (set! (.-title js/document)
            (str "Chat (" (inc (get-in state [:notifications :unread-count])) ")"))
      (update-in state [:notifications :unread-count] inc))
    state))

; error

(defn display-error
  ([state err-key msg]
   (display-error state err-key msg :error))
  ([state err-key msg cls]
   (update-in state [:errors] #(conj % [err-key msg cls]))))

(defn clear-error [state err-key]
  (update-in state [:errors] #(into [] (remove (fn [[k _]] (= k err-key))) %)))

; page

(defn set-group-and-page [state group-id page-id]
  (assoc state
    :open-group-id group-id
    :page page-id))

(defn set-page-error [state bool]
  (assoc-in state [:page :error?] bool))

(defn set-page-loading [state bool]
  (assoc-in state [:page :loading?] bool))

; session

(defn set-session [state data]
  (assoc-in state [:session] data))

(defn clear-session [state]
  (assoc-in state [:session] nil))

; user

(defn set-preferences [state prefs]
  (update-in state [:preferences] #(merge % prefs)))

; users

(defn add-users [state users]
  (update-in state [:users] #(merge % (key-by-id users))))

(defn add-user [state user]
  (update-in state [:users] #(assoc % (:id user) user)))

(defn update-user-nickname [state user-id nickname]
  (assoc-in state [:users user-id :nickname] nickname))

(defn update-user-avatar [state user-id avatar]
  (assoc-in state [:users user-id :avatar] avatar))

(defn update-user-status [state user-id status]
  (if (get-in state [:users user-id])
    (assoc-in state [:users user-id :status] status)
    state))

(defn remove-user-from-group [state user-id group-id]
  ; TODO: also remove user from collection if group-ids is now empty? shouldn't make a difference
  ; TODO: remove mentions of that user from the group?
  (update-in state [:users user-id :group-ids] (partial remove (partial = group-id))))

; admins

(defn make-user-admin [state group-id user-id]
  (update-in state [:groups group-id :admins] #(conj % user-id)))

(defn become-group-admin [state group-id]
  (-> state
      (update-in [:users (current-user-id state) :group-ids] #(vec (conj (set %) group-id)))
      (update-in [:groups group-id :admins] #(conj % (current-user-id state)))))

; threads and messages

(defn set-threads [state threads]
  (assoc-in state [:threads] (key-by-id threads)))

(defn reset-new-thread-id [state]
  (assoc-in state [:new-thread-id] (uuid/make-random-squuid)))

(defn maybe-reset-new-thread-id [state thread-id]
  (if (= thread-id (:new-thread-id state))
    (reset-new-thread-id state)
    state))

(defn set-open-threads [state threads]
  (-> state
      (assoc-in [:group-threads] (into {}
                                       (map (fn [[g t]]
                                              [g (into #{} (map :id) t)]))
                                       (group-by :group-id threads)))
      (assoc-in [:user :open-thread-ids] (set (map :id threads)))))

(defn focus-thread [state thread-id]
  (assoc-in state [:focused-thread-id] thread-id))

(defn update-thread-last-open-at [state thread-id]
  (if-let [thread (get-in state [:threads thread-id])]
    (let [latest-message (->> (thread :messages)
                              (map :created-at)
                              (reduce max (js/Date. 0)))
          new-last-open (js/Date. (inc (.getTime latest-message)))]
      (update-in state [:threads thread-id :last-open-at] (constantly new-last-open)))
    state))

(defn maybe-create-thread [state thread-id group-id]
  (let [state (update-in state [:user :open-thread-ids] #(conj % thread-id))]
    (if-not (get-in state [:threads thread-id])
      (-> state
          (assoc-in [:threads thread-id] {:id thread-id
                                          :group-id group-id
                                          :messages []
                                          :tag-ids #{}
                                          :mentioned-ids #{}})
          (update-in [:group-threads group-id] #(conj (set %) thread-id)))
      state)))

(defn add-message [state message]
  (-> state
      (maybe-create-thread (message :thread-id) (message :group-id))
      (update-in [:threads (message :thread-id) :messages] #(conj % message))
      (update-thread-last-open-at (message :thread-id))
      (update-in [:threads (message :thread-id) :tag-ids]
                 (partial set/union (set (message :mentioned-tag-ids))))
      (update-in [:threads (message :thread-id) :mentioned-ids]
                 (partial set/union (set (message :mentioned-user-ids))))))

(defn set-message-failed [state message]
  (update-in state [:threads (message :thread-id) :messages]
             (partial map (fn [msg] (if (= (message :id) (msg :id))
                                      (assoc msg :failed? true)
                                      msg)))))

(defn clear-message-failed [state message]
  (update-in state [:threads (message :thread-id) :messages]
    (partial map (fn [msg] (if (= (message :id) (msg :id))
                             (dissoc msg :failed?)
                             msg)))))

(defn add-threads [state threads]
  (-> state
      (update-in [:threads] #(merge-with merge % (key-by-id threads)))
      (update-in [:group-threads] #(merge-with
                                     set/union
                                     %
                                     (into {}
                                           (map (fn [[g t]]
                                                  [g (into #{} (map :id) t)]))
                                           (group-by :group-id threads))))))

(defn add-open-thread [state thread]
  (-> state
      (update-in [:threads (thread :id)] #(merge % thread))
      (update-in [:group-threads (thread :group-id)] #(conj (set %) (thread :id)))
      (update-in [:user :open-thread-ids] #(conj % (thread :id)))))

(defn hide-thread [state thread-id]
  (update-in state [:user :open-thread-ids] #(disj % thread-id)))

(defn show-thread [state thread-id]
  (update-in state [:user :open-thread-ids] #(conj % thread-id)))

; pages

(defn set-channel-results [state threads]
  (-> state
      (update-in [:threads] #(merge-with merge % (key-by-id threads)))
      (update-in [:page :thread-ids] (constantly (map :id threads)))))

(defn add-channel-results [state threads]
  (-> state
      (update-in [:threads] #(merge-with merge % (key-by-id threads)))
      (update-in [:page :thread-ids] #(concat % (map :id threads)))))

(defn set-pagination-remaining [state threads-count]
  (update-in state [:pagination-remaining] (constantly threads-count)))

; tags

(defn subscribe-to-tag [state tag-id]
  (update-in state [:user :subscribed-tag-ids] #(conj % tag-id)))

(defn unsubscribe-from-tag [state tag-id]
  (update-in state [:user :subscribed-tag-ids] #(disj % tag-id)))

(defn add-tag [state tag]
  (assoc-in state [:tags (tag :id)] tag))

(defn remove-tag [state tag-id]
  (-> state
      (update-in [:threads] (partial into {}
                                     (map (fn [[t-id t]]
                                            [t-id (update t :tag-ids disj tag-id)]))))
      (update-in [:tags] #(dissoc % tag-id))))

(defn add-tags [state tags]
  (update-in state [:tags] #(merge % (key-by-id tags))))

(defn set-tag-description [state tag-id description]
  (assoc-in state [:tags tag-id :description] description))

(defn set-subscribed-tag-ids [state tag-ids]
  (assoc-in state [:user :subscribed-tag-ids] (set tag-ids)))

; new thread msg

(defn set-new-message [state thread-id content]
  (if (get-in state [:threads thread-id])
    (assoc-in state [:threads thread-id :new-message] content)
    (assoc-in state [:new-thread-msg thread-id] content)))

; search

(defn set-search-results [state query {:keys [threads thread-ids]}]
  (-> state
      (update-in [:threads] #(merge-with merge % (key-by-id threads)))
      (update-in [:page] (fn [p] (if (= (p :search-query) query)
                                   (assoc p :thread-ids thread-ids)
                                   p)))))

(defn set-search-query [state query]
  (assoc-in state [:page :search-query] query))

; groups

(defn set-groups [state groups]
  (assoc-in state [:groups] (key-by-id groups)))

(defn add-group [state group]
  (update-in state [:groups] (flip assoc (group :id) group)))

(defn remove-group [state group-id]
  (let [group-threads (get-in state [:group-threads group-id])]
    (-> state
        (update-in [:threads] #(apply dissoc % group-threads))
        (update-in [:group-threads] (flip dissoc group-id))
        (update-in [:groups] (flip dissoc group-id)))))

(defn set-group-publicity [state group-id publicity]
  (assoc-in state [:groups group-id :public?] publicity))

(defn set-group-intro [state group-id intro]
  (assoc-in state [:groups group-id :intro] intro))

(defn set-group-avatar [state group-id avatar]
  (assoc-in state [:groups group-id :avatar] avatar))

; invitations

(defn set-invitations [state invitations]
  (assoc-in state [:invitations] invitations))

(defn add-invite [state invite]
  (update-in state [:invitations] #(conj % invite)))

(defn remove-invite [state invite]
  (update-in state [:invitations] (partial remove (partial = invite))))

; bots

(defn add-group-bot [state group-id bot]
  (update-in state [:groups group-id :bots] #(conj % bot)))
