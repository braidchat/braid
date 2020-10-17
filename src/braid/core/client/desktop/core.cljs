(ns braid.core.client.desktop.core
  (:require
   [braid.base.client.events]
   [braid.base.client.subs]
   [braid.chat.client.subs]
   [braid.core.client.core.events]
   [braid.core.client.gateway.events]
   [braid.core.client.gateway.subs]
   [braid.core.client.group-admin.events]
   [braid.core.client.group-admin.subs]
   [braid.core.client.invites.events]
   [braid.core.client.invites.subs]
   [braid.base.client.router :as router]
   [braid.core.client.state.remote-handlers]
   [braid.core.client.ui.views.app :refer [app-view]]
   [braid.core.modules :as modules]
   [re-frame.core :as rf :refer [dispatch-sync dispatch]]
   [reagent.core :as r]))

(enable-console-print!)

(defn render []
  (r/render [app-view] (.getElementById js/document "app")))

(defn ^:export init []
  (modules/init!)
  (dispatch-sync [:initialize-db])

  (.addEventListener js/document "visibilitychange"
                     (fn [e]
                       (dispatch [:set-window-visibility
                                  (= "visible" (.-visibilityState js/document))])))

  (render)

  (router/init))

(defn ^:export reload
  "Force a re-render. For use with figwheel"
  []
  (modules/init!)
  (render))
