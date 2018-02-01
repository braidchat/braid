(ns braid.client.gateway.core
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch-sync]]
    [braid.client.gateway.events]
    [braid.client.gateway.subs]
    [braid.client.gateway.views :refer [gateway-view]]
    [braid.core.modules :as modules]))

(enable-console-print!)

(defn render []
  (r/render [gateway-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (modules/init!)
  (dispatch-sync [:braid.client.gateway.events/initialize (keyword (aget js/window "gateway_action"))])
  (render))

(defn ^:export reload []
  (render))
