(ns braid.core.client.desktop.notify
  (:require
   [goog.object :as o]))

(defn has-notify?
  []
  (some? (o/get js/window "Notification")))

(defn enabled?
  []
  (= "granted" (.-permission js/Notification)))

(defn request-permission
  [cb]
  (.requestPermission js/Notification (fn [perm] (cb perm))))

(defn notify
  [{:keys [title body icon] :or {title "Braid"
                                 icon "https://braid.chat/images/braid.svg"}}]
  (js/Notification. title #js {:body body :icon icon}))
