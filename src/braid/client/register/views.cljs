(ns braid.client.register.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [braid.client.register.styles :as styles]
    [braid.client.register.views.create_group :refer [group-create-view]]
    [braid.client.register.views.user :refer [user-auth-view]]
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

(defn form-view []
  [:form.register {:on-submit (fn [e]
                                (.preventDefault e)
                                (dispatch [:submit-form]))}
   [header-view]
   [user-auth-view]
   [group-create-view]])

(defn app-view []
  [:div.app
   [style-view]
   [form-view]])
