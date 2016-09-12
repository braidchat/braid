(ns braid.client.mobile.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch-sync dispatch]]
            [braid.client.mobile.views :refer [app-view]]
            [braid.client.router :as router]
            braid.client.mobile.auth-flow.routes
            braid.client.routes
            braid.client.state.remote-handlers
            braid.client.mobile.auth-flow.subs
            braid.client.mobile.auth-flow.events
            braid.client.uploads.subs
            braid.client.uploads.events
            braid.client.core.subs
            braid.client.core.events))

(enable-console-print!)

(defn render []
  (r/render [app-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (dispatch-sync [:initialize-db])
  (render)
  (router/init)
  (dispatch [:check-auth]))

(defn ^:export reload []
  (render))
