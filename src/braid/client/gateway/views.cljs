(ns braid.client.gateway.views
  (:require
    [re-frame.core :refer [subscribe]]
    [garden.core :refer [css]]
    [braid.client.gateway.styles :as styles]
    [braid.client.ui.styles.imports :refer [imports]]
    [braid.client.gateway.forms.create-group.views :refer [create-group-view]]
    [braid.client.gateway.forms.create-group.styles :refer [create-group-styles]]
    [braid.client.gateway.forms.join-group.views :refer [join-group-view]]
    [braid.client.gateway.forms.join-group.styles :refer [join-group-styles]]
    [braid.client.gateway.forms.user-auth.views :refer [user-auth-view]]
    [braid.client.gateway.forms.user-auth.styles :refer [user-auth-styles]]))

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
           (join-group-styles)
           (user-auth-styles)])}}])

(defn header-view []
  [:h1.header "Braid"])

(defn gateway-view []
  [:div.gateway
   [style-view]
   [header-view]
   [user-auth-view]
   (case @(subscribe [:braid.client.gateway.subs/action])
     :create-group [create-group-view]
     :join-group [join-group-view]
     nil)])

