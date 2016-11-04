(ns braid.client.gateway.core
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch-sync]]
    [braid.client.gateway.events]
    [braid.client.gateway.subs]
    [braid.client.gateway.views :refer [app-view]]))

(enable-console-print!)

(defn render []
  (r/render [app-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (dispatch-sync [:gateway/initialize])
  (render))

(defn ^:export reload []
  (render))
