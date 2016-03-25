(ns braid.mobile.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [braid.mobile.state]
            [braid.mobile.style :refer [styles]]
            [braid.mobile.sidebar :refer [sidebar-view]]
            [braid.mobile.panels :refer [panels-view]]))

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
        [panels-view @threads thread-view]]])))

(defn groups-view []
  (let [groups (subscribe [:groups-with-unread])
        active-group (subscribe [:active-group])]
    [:div.groups
     (doall
       (for [group @groups]
         ^{:key (group :id)}
         [:a.group {:class (when (= (group :id)
                                    (@active-group :id))
                             "active")
                    :on-click
                    (fn [e]
                      (dispatch [:set-active-group-id! (group :id)]))}
          [:img]
          (when (not= 0 (:unread-count group))
            [:div.badge (:unread-count group)])]))]))

(defn main-view []
  [:div.main
   [sidebar-view
    [:div [groups-view]]]
   [inbox-view]])

(defn login-view []
  [:div.login
   [:input {:placeholder "email"}]
   [:input {:placeholder "password"}]
   [:button {:on-click (fn [_] (dispatch [:log-in!]))}
    "Let's Do This!"]])

(defn app-view []
  (let [logged-in? (subscribe [:logged-in?])]
    (fn []
      [:div.app
       [:style {:type "text/css"
                :dangerouslySetInnerHTML {:__html styles}}]
       (if @logged-in?
         [main-view]
         [login-view])])))

(defn init []
  (r/render [app-view] (.-body js/document)))

(init)
