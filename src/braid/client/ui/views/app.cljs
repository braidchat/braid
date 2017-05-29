(ns braid.client.ui.views.app
  (:require
    [re-frame.core :refer [subscribe]]
    [braid.client.gateway.views :refer [gateway-view]]
    [braid.client.ui.views.main :refer [main-view]]
    [braid.client.ui.views.styles :refer [styles-view]]))

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
