(ns braid.profile.core
  "Allows users to set and update their profile."
  (:require
   [braid.core.api :as core]
   #?@(:cljs
       [[reagent.core :as r]])))

(defn profile-view
  []
  #?(:cljs
  (let [format-error (r/atom false)
        error (r/atom nil)
        set-format-error! (fn [error?] (reset! format-error error?))
        set-error! (fn [err] (reset! error err))
        profile "foo"
        new-profile (r/atom "")]
    (fn []
      [:div.setting
       [:h2 "Update profile"]
       [:div.profile
        (when profile
          [:div.current-profile profile])
        (when @error
          [:span.error @error])
        [:form {:on-submit (fn [e]
                             (.preventDefault e)
                             (set-error! nil)
                             (js/window.alert (str "submitted" @new-profile)))}
         [:input.new-profile
          {:class (when @format-error "error")
           :placeholder profile
           :value @new-profile
           :on-change (fn [e]
                        (->> (.. e -target -value)
                             (reset! new-profile)))}]
         [:input {:type "submit" :value "Update"}]]]]))))

(defn init! []
  #?(:cljs
     (do
       (core/register-user-profile-item!
        {:priority 10
         :view profile-view}))))
