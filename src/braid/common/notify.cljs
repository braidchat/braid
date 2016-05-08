(ns braid.common.notify)

(defn enabled?
  []
  (= "granted" (.-permission js/Notification)))

(defn request-permission
  [cb]
  (.requestPermission js/Notification (fn [perm] (cb perm))))

(defn notify
  [{:keys [title msg icon] :or {title "Braid.Chat"
                                icon "https://braid.chat/images/braid.svg"}}]
  (js/Notification. title (clj->js {:body msg :icon icon})))
