(ns braid.client.mobile.core
  (:require
    [braid.client.core.events]
    [braid.client.core.subs]
    [braid.client.gateway.events]
    [braid.client.gateway.subs]
    [braid.client.mobile.auth-flow.events]
    [braid.client.mobile.auth-flow.routes]
    [braid.client.mobile.auth-flow.subs]
    [braid.client.mobile.views :refer [app-view]]
    [braid.client.router :as router]
    [braid.client.routes]
    [braid.client.state.remote-handlers]
    [braid.client.uploads.events]
    [braid.client.uploads.subs]
    [re-frame.core :refer [dispatch-sync dispatch]]
    [reagent.core :as r]))

(enable-console-print!)

(defn render []
  (r/render [app-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (dispatch-sync [:initialize-db])
  (render)
  (router/init))

(defn ^:export reload []
  (render))
