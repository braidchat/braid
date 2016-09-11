(ns braid.client.mobile.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [braid.client.mobile.state]
            [braid.client.mobile.style :refer [styles]]
            [braid.client.helpers :refer [->color]]
            [braid.client.routes :as routes]
            [braid.client.ui.views.sidebar]
            [braid.client.ui.views.thread :refer [messages-view]]
            [braid.client.ui.views.new-message :refer [upload-button-view]]
            [braid.client.ui.views.header :refer [group-name-view group-header-buttons-view]]
            [retouch.core :refer [drawer-view swipe-view]])
  (:import [goog.events KeyCodes]))

(defn new-message-view [config]
  (let [message (r/atom "")
        group-id (subscribe [:open-group-id])]
    (fn [config]
      [:div.new.message
       [upload-button-view {:thread-id (config :thread-id)
                            :group-id @group-id}]
       [:textarea {:value @message
                   :placeholder (config :placeholder)
                   :on-change
                   (fn [e]
                     (reset! message (.. e -target -value)))
                   :on-key-up
                   (fn [e]
                     (condp = (.-keyCode e)
                       KeyCodes.ENTER
                       (do
                         (dispatch [:new-message {:group-id @group-id
                                                  :thread-id (config :thread-id)
                                                  :mentioned-tag-ids (config :mentioned-tag-ids)
                                                  :mentioned-user-ids (config :mentioned-user-ids)
                                                  :content @message}])

                         (reset! message ""))
                       nil))}]])))

(defn thread-view [thread]
  (let [open? (subscribe [:thread-open? (thread :id)])]
    (fn [thread]
      [:div.thread
       [:div.card
        [:div.head
         [braid.client.ui.views.thread/thread-tags-view thread]
         (when @open?
           [:div.close {:on-click (fn [_]
                                    (dispatch [:hide-thread {:thread-id (thread :id)}]))}])]]

       [messages-view thread]
       (println thread)
       [new-message-view {:thread-id (thread :id)
                          :placeholder (if (thread :new?)
                                         "Start a conversation..."
                                         "Reply...")
                          :mentioned-user-ids (when (thread :new?)
                                                (thread :mentioned-ids))
                          :mentioned-tag-ids (when (thread :new?)
                                               (thread :tag-ids))
                          }]])))

(defn header-view []
  (let [group-id (subscribe [:open-group-id])]
    (fn []
      [:div.group-header {:style {:background-color (->color @group-id)}}
       [:a.open-sidebar {:on-click (fn []
                                     ; TODO open sidebar
                                     )}]
       [group-name-view]
       [:span.spacer]
       [group-header-buttons-view [{:title "Inbox"
                                    :route-fn routes/inbox-page-path
                                    :class "inbox"}]]])))

(defn inbox-view []
  (let [group-id (subscribe [:open-group-id])
        threads (subscribe [:open-threads] [group-id])
        temp-thread (subscribe [:temp-thread])]
    (fn []
      [:div.inbox.page
       [header-view]
       [:div.threads
        [swipe-view (conj @threads
                          @temp-thread) thread-view]]])))

(defn main-view []
  [:div.main
   [drawer-view
    [:div.sidebar
     [braid.client.ui.views.sidebar/groups-view]]]
   [inbox-view]])

(defn login-flow-view []
  (let [method (r/atom nil) ; :login or :register
        stage (r/atom nil)  ; :email or :password
        email (r/atom nil)
        password (r/atom nil)]
    (fn []
      [:div.login-flow
       (case @method
         nil
         [:div.content.welcome
          [:img.logo {:src "/images/braid.svg"}]
          [:button.login
           {:on-click (fn [_]
                        (reset! method :login)
                        (reset! stage :email))}
           "Log In"]
          [:button.register
           {:on-click (fn [_]
                        (reset! method :register))}
           "Register"]]

         :login
         (case @stage
           :email
           [:form.content.login.email
            {:on-submit (fn [e]
                          (.preventDefault e)
                          (reset! stage :password))}
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
                                      :on-complete (fn [])
                                      :on-error (fn [])}]))}
            [:label.password
             "Password"
             [:input.password {:placeholder "••••••"
                               :type "password"
                               :key "password"
                               :auto-focus true
                               :on-change (fn [e]
                                            (reset! password (.. e -target -value)))}]]
            [:button.next {:type "submit"} "Next"]])

         :register
         (if-not @email
           [:div.content.register.email
            [:input.email {:placeholder "you@awesome.com"
                           :auto-focus true}]
            [:button.next {:on-click (fn [e]
                                       (reset! email (.. e -target -value))
                                       ; TODO
                                       )}]]))])))

(defn style-view []
  [:style {:type "text/css"
           :dangerouslySetInnerHTML {:__html styles}}])

(defn app-view []
  (let [login-state (subscribe [:login-state])]
    (fn []
      [:div.app
       [style-view]
       (case @login-state
         :auth-check
         [:div.status.authenticating "Authenticating..."]

         :ws-connect
         [:div.status.connecting "Connecting..."]

         :login-form
         [login-flow-view]

         :app
         [main-view])])))


