(ns chat.client.store)

(def app-state (atom {:messages {}
                      :users {}
                      :tags {}
                      :session nil
                      :open-thread-ids #{}}))

(defn transact! [ks f]
  (swap! app-state update-in ks f))

(defn add-message! [message]
  (transact! [:messages] #(assoc % (message :id) message)))

(defn set-session! [session]
  (transact! [:session] (constantly session)))

(defn clear-session! []
  (transact! [:session] (constantly nil)))

(defn add-messages! [messages]
  (transact! [:messages] #(merge % (->> messages
                                       (reduce (fn [memo m]
                                                 (assoc memo (m :id) m)) {})))))
(defn add-users! [users]
  (transact! [:users] #(merge % (->> users
                                     (reduce (fn [memo u]
                                               (assoc memo (u :id) u)) {})))))

(defn add-tags! [tags]
  (transact! [:tags] #(merge % (->> tags
                                     (reduce (fn [memo t]
                                               (assoc memo (t :id) t)) {})))))

(defn set-open-thread-ids! [thread-ids]
  (transact! [:open-thread-ids] (constantly (set thread-ids))))

(defn hide-thread! [thread-id]
  (transact! [:open-thread-ids] #(disj % thread-id)))

(defn show-thread! [thread-id]
  (transact! [:open-thread-ids] #(conj % thread-id)))
