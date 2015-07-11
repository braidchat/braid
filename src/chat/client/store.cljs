(ns chat.client.store)

(def app-state (atom {:messages []}))

(defn transact! [ks f]
  (swap! app-state update-in ks f))

(defn add-message! [message]
  (transact! [:messages] #(assoc % (message :id) message)))
