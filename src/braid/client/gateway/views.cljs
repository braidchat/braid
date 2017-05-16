(ns braid.client.gateway.views
  (:require
    [re-frame.core :refer [subscribe]]
    [garden.core :refer [css]]
    [braid.client.gateway.styles :as styles]
    [braid.client.ui.styles.imports :refer [imports]]
    [braid.client.gateway.views.join-group :refer [join-group-view]]
    [braid.client.gateway.create-group.views :refer [create-group-view]]
    [braid.client.gateway.create-group.styles :refer [create-group-styles]]
    [braid.client.gateway.user-auth.views :refer [user-auth-view]]
    [braid.client.gateway.user-auth.styles :refer [user-styles]]))

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
          (styles/form-styles)
          (create-group-styles)
          (user-styles))}}])

(defn header-view []
  [:h1.header "Braid"])

(defn gateway-view []
  [:div.gateway
   [style-view]
   [header-view]
   [user-auth-view]
   (case @(subscribe [:gateway/action])
     :create-group [create-group-view]
     :join-public-group [join-group-view :public]
     :join-private-group [join-group-view :private]
     nil)])

