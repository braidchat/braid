(ns braid.core.client.gateway.core
  (:require
   [braid.core.client.gateway.events :as events]
   [braid.core.client.gateway.subs]
   [braid.core.client.gateway.views :refer [gateway-view]]
   [braid.core.modules :as modules]
   [goog.object :as o]
   [re-frame.core :refer [dispatch-sync]]
   [reagent.dom :as r-dom]))

(enable-console-print!)

(defn render []
  (r-dom/render [gateway-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (modules/init! modules/default)
  (dispatch-sync [::events/initialize (keyword (o/get js/window "gateway_action"))])
  (render))

(defn ^:export reload []
  (render))
