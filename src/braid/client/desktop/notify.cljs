(ns braid.client.desktop.notify)

(defn has-notify?
  []
  (some? (aget js/window "Notification")))

(defn enabled?
  []
  (= "granted" (.-permission js/Notification)))

(defn request-permission
  [cb]
  (.requestPermission js/Notification (fn [perm] (cb perm))))

(defn notify
  [{:keys [title msg icon] :or {title "Braid"
                                icon "https://braid.chat/images/braid.svg"}}]
  (js/Notification. title (clj->js {:body msg :icon icon})))
