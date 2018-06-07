(ns braid.core.client.gateway.views
  (:require
   [braid.core.client.gateway.forms.create-group.styles :refer [create-group-styles]]
   [braid.core.client.gateway.forms.create-group.views :refer [create-group-view]]
   [braid.core.client.gateway.forms.user-auth.styles :refer [user-auth-styles]]
   [braid.core.client.gateway.forms.user-auth.views :refer [user-auth-view]]
   [braid.core.client.gateway.styles :as styles]
   [braid.core.client.ui.styles.imports :refer [imports]]
   [garden.core :refer [css]]
   [re-frame.core :refer [subscribe]]))

(defn style-view []
  [:style
   {:type "text/css"
    :dangerouslySetInnerHTML
    {:__html
     (css {:auto-prefix #{:transition
                          :flex-direction
                          :flex-shrink
                          :align-items
                          :animation
                          :flex-grow}
           :vendors ["webkit"]}
          imports
          styles/anim-spin
          (styles/app-styles)
          [:#app
           (styles/form-styles)
           (create-group-styles)
           (user-auth-styles)])}}])

(defn header-view []
  [:h1.header "Braid"])

(defn gateway-view []
  [:div.gateway
   [style-view]
   [header-view]
   [user-auth-view]
   (case @(subscribe [:braid.core.client.gateway.subs/action])
     :create-group [create-group-view]
     nil)])
