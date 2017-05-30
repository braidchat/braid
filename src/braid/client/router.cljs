(ns braid.client.router
  (:require
    [accountant.core :as accountant]
    [secretary.core :as secretary])
  (:import
    [goog Uri]))

(defn init []
  (accountant/configure-navigation!
    {:nav-handler (fn [path]
                    (secretary/dispatch! path))
     :path-exists? (fn [path]
                     (secretary/locate-route path))}))

(defn dispatch-current-path! []
  (accountant/dispatch-current!))

(defn go-to [path]
  (accountant/navigate! path))

(defn current-path []
  (.getPath (.parse Uri js/window.location)))
