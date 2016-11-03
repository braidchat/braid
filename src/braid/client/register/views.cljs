(ns braid.client.register.views
  (:require
    [re-frame.core :refer [subscribe]]
    [garden.core :refer [css]]
    [garden.stylesheet :refer [at-import]]
    [braid.client.register.styles :as styles]
    [braid.client.register.views.join-group :refer [join-group-view]]
    [braid.client.register.views.reset-password :refer [reset-password-view]]
    [braid.client.register.create-group.views :refer [create-group-view]]
    [braid.client.register.create-group.styles :refer [create-group-styles]]
    [braid.client.register.user-auth.views :refer [user-auth-view]]
    [braid.client.register.user-auth.styles :refer [user-styles]]))

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
          (at-import "https://fonts.googleapis.com/css?family=Open+Sans:400,700")
          styles/anim-spin
          (styles/app-styles)
          (styles/form-styles)
          (create-group-styles)
          (user-styles))}}])

(defn header-view []
  [:h1.header "Braid"])

(defn form-view []
  (let [action-mode (subscribe [:register/action-mode])]
    (fn []
      [:div.register
       [header-view]
       [user-auth-view]
       (case @action-mode
         :create-group [create-group-view]
         :join-public-group [join-group-view :public]
         :join-private-group [join-group-view :private]
         :reset-password [reset-password-view])])))

(defn app-view []
  [:div.app
   [style-view]
   [form-view]])
