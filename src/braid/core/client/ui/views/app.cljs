(ns braid.core.client.ui.views.app
  (:require
   [braid.core.client.ui.views.main :refer [main-view]]
   [braid.core.client.ui.views.styles :refer [styles-view]]
   [re-frame.core :refer [subscribe]]))

(defn app-view []
  (case @(subscribe [:login-state])

    (:ws-connect :anon-ws-connect)
    [:div.status
     [styles-view]
     [:span "Connecting..."]]

    (:gateway :app :anon-connected)
    [:div.app
     [styles-view]
     [main-view]]))
