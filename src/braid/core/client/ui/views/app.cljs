(ns braid.core.client.ui.views.app
  (:require
   [braid.core.client.gateway.views :refer [gateway-view]]
   [braid.core.client.ui.views.main :refer [main-view]]
   [braid.core.client.ui.views.styles :refer [styles-view]]
   [re-frame.core :refer [subscribe]]))

(defn app-view []
  (case @(subscribe [:login-state])

    :gateway
    [gateway-view]

    :ws-connect
    [:div.status
     [styles-view]
     [:span "Connecting..."]]

    :app
    [:div.app
     [styles-view]
     [main-view]]))
