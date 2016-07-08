(ns braid.client.mobile.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [braid.client.mobile.state]
            [braid.client.mobile.style :refer [styles]]
            [braid.client.ui.views.sidebar]
            [retouch.core :refer [drawer-view swipe-view]]))

(enable-console-print!)

(defn message-view [message]
  [:div.message
   [:a.avatar
    [:img]]
   [:div.info
    [:div.nickname "username"]
    [:div.time "4:35 PM"]]
   [:div.content (:content message)]])

(defn new-message-view []
  (fn []
    [:div.new.message
     [:textarea {:value "asd"
                 :on-change (fn [e])}]]))

(defn tag-view [tag-id]
  (let [tag (subscribe [:get-tag tag-id])]
    [:div.tag (@tag :name)]))

(defn thread-view [thread]
  [:div.thread
   [:div.card
    [:div.head
     [:div.tags
      (doall
        (for [tag-id (thread :tag-ids)]
          ^{:key tag-id}
          [tag-view tag-id]))
      [:a.add "+"]]
     [:div.close "×"]]]
   [:div.messages
    (doall
      (for [message (:messages thread)]
        ^{:key (message :id)}
        [message-view message]))]
   [new-message-view] ])

(defn inbox-view []
  (let [threads (subscribe [:active-group-inbox-threads])]
    (fn []
      [:div.inbox.page
       [:div.threads
        [swipe-view @threads thread-view]]])))

(defn main-view []
  [:div.main
   [drawer-view
    [:div.sidebar
     [braid.client.ui.views.sidebar/groups-view {:subscribe subscribe}]]]
   [inbox-view]])

(defn login-flow-view []
  (let [data (r/atom {})]
    (fn []
      [:div.login-flow
       (if-not (@data :method)
         [:div.content.welcome
          [:img.logo {:src "/images/braid.svg"}]
          [:button.login
           {:on-click (fn [_]
                        (swap! data assoc :method :login))}
           "Log In"]
          [:button.register
           {:on-click (fn [_]
                        (swap! data assoc :method :register))}
           "Register"]]
         (case (@data :method)
           :login
           (if-not (@data :email)
             [:div.content.login.email
              "Email"
              [:input.email {:placeholder "you@awesome.com"
                             :type "email"
                             :key "email"}]
              [:button.next {:on-click (fn [e]
                                         (swap! data assoc :email (.. e -target -value)))}]]
             [:div.content.login.password
              "Password"
              [:input.password {:placeholder "••••••"
                                :type "password"
                                :key "password"}]
              [:button.next {:on-click (fn [e]
                                         (swap! data assoc :email (.. e -target -value))
                                         (dispatch [:log-in!]))}]])
           :register
           (if-not (@data :email)
             [:div.content.register.email
              [:input.email {:placeholder "you@awesome.com"}]
              [:button.next {:on-click (fn [e]
                                         (swap! data assoc :email (.. e -target -value))
                                         (dispatch [:log-in!]))}]])))])))

(defn style-view []
  [:style {:type "text/css"
           :dangerouslySetInnerHTML {:__html styles}}])

(defn app-view []
  (let [logged-in? (subscribe [:logged-in?])]
    (fn []
      [:div.app
       [style-view]
       (if @logged-in?
         [main-view]
         [login-flow-view])])))

(defn init []
  (r/render [app-view] (.-body js/document)))

(init)
