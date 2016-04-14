(ns braid.ui.views.app
  (:require [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.styles :refer [styles-view]]
            [braid.ui.views.main :refer [main-view]]
            [braid.ui.views.login :refer [login-view]]))

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
