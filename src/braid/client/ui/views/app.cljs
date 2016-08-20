(ns braid.client.ui.views.app
  (:require [braid.client.state :refer [subscribe]]
            [braid.client.ui.views.styles :refer [styles-view]]
            [braid.client.ui.views.main :refer [main-view]]
            [braid.client.ui.views.login :refer [login-view]]))

(defn app-view []
  (let [login-state (subscribe [:login-state])]
    (fn []
      [:div.app
       [styles-view]

       (case @login-state
         :auth-check
         [:div.status.authenticating "Authenticating..."]

         :ws-connect
         [:div.status.ws-connet "Connecting..."]

         :login-form
         [login-view]

         :app
         [main-view])])))
