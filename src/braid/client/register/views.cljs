(ns braid.client.register.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [braid.client.register.styles :as styles]
    [braid.client.register.views.create-group :refer [create-group-view]]
    [braid.client.register.views.join-group :refer [join-group-view]]
    [braid.client.register.views.user :refer [user-auth-view]]
    [braid.client.register.views.reset-password :refer [reset-password-view]]
    [garden.core :refer [css]]
    [garden.stylesheet :refer [at-import]]
    [braid.client.register.styles.create-group :refer [create-group-styles]]
    [braid.client.register.styles.user :refer [user-styles]]))

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
  [:h1 "Braid"])

(def mode :create-group)

(defn form-view []
  [:form.register {:on-submit (fn [e]
                                (.preventDefault e)
                                (dispatch [:submit-form]))}
   [header-view]
   [user-auth-view]
   (case mode
     :create-group [create-group-view]
     :join-public-group [join-group-view :public]
     :join-private-group [join-group-view :private]
     :reset-password [reset-password-view])])

(defn app-view []
  [:div.app
   [style-view]
   [form-view]])
