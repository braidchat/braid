(ns braid.client.desktop.core
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf :refer [dispatch-sync dispatch]]
    [braid.client.bots.events]
    [braid.client.bots.subs]
    [braid.client.core.events]
    [braid.client.core.subs]
    [braid.client.gateway.events]
    [braid.client.gateway.subs]
    [braid.client.group-admin.events]
    [braid.client.group-admin.subs]
    [braid.client.invites.events]
    [braid.client.invites.subs]
    [braid.client.router :as router]
    [braid.client.state.remote-handlers]
    [braid.client.ui.views.app :refer [app-view]]
    [braid.client.uploads.events]
    [braid.client.uploads.subs]
    [braid.core.modules :as modules]))

(enable-console-print!)

(defn render []
  (r/render [app-view] (.getElementById js/document "app")))

(defn ^:export init []
  (dispatch-sync [:initialize-db])

  (.addEventListener js/document "visibilitychange"
                     (fn [e]
                       (dispatch [:set-window-visibility
                                  (= "visible" (.-visibilityState js/document))])))
  (modules/init!)

  (render)

  (router/init))

(defn ^:export reload
  "Force a re-render. For use with figwheel"
  []
  (render))
