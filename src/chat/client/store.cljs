(ns chat.client.store)

(def app-state (atom {:messages {}
                      :users {}}))

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
