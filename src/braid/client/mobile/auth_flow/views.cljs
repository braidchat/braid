(ns braid.client.mobile.auth-flow.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [braid.client.mobile.auth-flow.routes :refer [auth-flow-path]]))

; TODO
(defn register-flow-view []
  (let [stage (subscribe [:auth-flow-stage])
        email (r/atom nil)]
    (fn []
      (case @stage
        :email
        [:form.content.register.email
         {:on-submit (fn [e]
                       (.preventDefault e)
                       (dispatch [:go-to (auth-flow-path {:method :register
                                                          :stage :password})]))}
         [:input.email {:placeholder "you@awesome.com"
                        :auto-focus true
                        :on-change (fn [e]
                                     (reset! email (.. e -target -value)))}]
         [:button.next {:type "submit"} "Next"]]))))

(defn login-flow-view []
  (let [stage (subscribe [:auth-flow-stage])
        email (r/atom nil)
        password (r/atom nil)]
    (fn []
      (case @stage
        :email
        [:form.content.login.email
         {:on-submit (fn [e]
                       (.preventDefault e)
                       (dispatch [:go-to (auth-flow-path {:method "login"
                                                          :stage "password"})]))}
         [:label.email "Email"
          [:input.email {:placeholder "you@awesome.com"
                         :type "email"
                         :key "email"
                         :auto-focus true
                         :on-change (fn [e]
                                      (reset! email (.. e -target -value)))}]]
         [:button.next {:type "submit"} "Next"]]

        :password
        [:form.content.login.password
         {:on-submit (fn [e]
                       (.preventDefault e)
                       (dispatch [:auth
                                  {:email @email
                                   :password @password
                                   :on-complete (fn []
                                                  (dispatch [:go-to "/"]))
                                   :on-error (fn [])}]))}
         [:label.password
          "Password"
          [:input.password {:placeholder "••••••"
                            :type "password"
                            :key "password"
                            :auto-focus true
                            :on-change (fn [e]
                                         (reset! password (.. e -target -value)))}]]
         [:button.next {:type "submit"} "Next"]]))))

(defn welcome-view []
  [:div.content.welcome
   [:img.logo {:src "/images/braid.svg"}]
   [:button.login
    {:on-click (fn [_]
                 (dispatch [:go-to (auth-flow-path {:method "login"
                                                    :stage "email"})]))}
    "Log In"]
   ; disable until we have registration flow complete
   #_[:button.register
      {:on-click (fn [_]
                   (dispatch [:go-to (auth-flow-path {:method "register"
                                                      :stage "email"})]))}
      "Register"]])

(defn auth-flow-view []
  (let [method (subscribe [:auth-flow-method])]
    (fn []
      [:div.auth-flow
       (case @method
         nil
         [welcome-view]

         :login
         [login-flow-view]

         :register
         [register-flow-view])])))
