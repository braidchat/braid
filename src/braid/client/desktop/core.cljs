(ns braid.client.desktop.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf :refer [dispatch-sync]]
            [braid.client.ui.views.app :refer [app-view]]
            [braid.client.clj-highlighter :as highlighter]
            [braid.client.state.remote-handlers]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.router :as router]
            braid.client.subs
            braid.client.events
            braid.client.quests.subscriptions
            braid.client.quests.handlers
            [braid.client.quests.handler :as quests]))

(enable-console-print!)

(defn ^:export init []
  (dispatch-sync [:initialize-db])

  (.addEventListener js/document "visibilitychange"
                     (fn [e]
                       (dispatch! :set-window-visibility
                                  [(= "visible" (.-visibilityState js/document))])))

  (highlighter/install-highlighter)

  (r/render [app-view] (. js/document (getElementById "app")))

  (router/init)

  (dispatch! :check-auth)

  (quests/install-quests-handler!))

(defn ^:export reload
  "Force a re-render. For use with figwheel"
  []
  (rf/remove-post-event-callback :quest-handler)
  (quests/install-quests-handler!)
  (r/render [app-view] (.getElementById js/document "app")))
