(ns braid.ui.views.app
  (:require [braid.ui.views.styles :refer [styles-view]]
            [braid.ui.views.main :refer [main-view]]
            [braid.ui.views.login :refer [login-view]]))

(defn app-view []
  [:div.app
    [styles-view]
    (case (data :login-state)
      :auth-check
      [:div.status.authenticating "Authenticating..."]
      :ws-connect
      [:div.status.ws-connet "Connecting..."]
      :login-form
      [login-view]
      :app
      [main-view])])
