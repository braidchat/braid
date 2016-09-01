(ns braid.client.desktop.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf :refer [dispatch-sync dispatch]]
            [braid.client.ui.views.app :refer [app-view]]
            [braid.client.clj-highlighter :as highlighter]
            [braid.client.state.remote-handlers]
            [braid.client.router :as router]
            braid.client.core.subs
            braid.client.core.events
            braid.client.quests.subs
            braid.client.quests.events
            braid.client.bots.subs
            braid.client.bots.events
            braid.client.uploads.subs
            braid.client.uploads.events
            braid.client.invites.subs
            braid.client.invites.events
            braid.client.group-admin.subs
            braid.client.group-admin.events
            [braid.client.quests.handler :as quests]))

(enable-console-print!)

(defn ^:export init []
  (dispatch-sync [:initialize-db])

  (.addEventListener js/document "visibilitychange"
                     (fn [e]
                       (dispatch [:set-window-visibility
                                  (= "visible" (.-visibilityState js/document))])))

  (highlighter/install-highlighter)

  (r/render [app-view] (. js/document (getElementById "app")))

  (router/init)

  (dispatch [:check-auth])

  (quests/install-quests-handler!))

(defn ^:export reload
  "Force a re-render. For use with figwheel"
  []
  (quests/install-quests-handler!)
  (r/render [app-view] (.getElementById js/document "app")))
