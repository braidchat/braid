(ns braid.client.register.core
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch-sync]]
    [braid.client.register.events]
    [braid.client.register.subs]
    [braid.client.register.views :refer [app-view]]))

(enable-console-print!)

(defn render []
  (r/render [app-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (dispatch-sync [:register/initialize])
  (render))

(defn ^:export reload []
  (render))
