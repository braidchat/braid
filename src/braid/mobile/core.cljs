(ns braid.mobile.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [braid.mobile.state]
            [braid.mobile.style :refer [styles]]))

(enable-console-print!)

(defn message-view [message]
  [:div.message
   (:content message)])

(defn thread-view [thread]
  [:div.thread
   [:div.close "X"]
   (for [message (:messages thread)]
     ^{:key (message :id)}
     [message-view message])
   [:textarea {:value "asd"}]])

(defn threads-view [threads]
  [:div.threads
   (for [thread threads]
     ^{:key (thread :id)}
     [thread-view thread])])

(defn inbox-view []
  (let [threads (subscribe [:threads])]
    [:div.inbox.page
     [threads-view @threads]]))

(defn groups-view []
  (let [groups (subscribe [:groups])]
    [:div.groups
     (for [group @groups]
       ^{:key (group :id)}
       [:a.group
        [:img]
        [:div.name (group :name)]])]))

(defn sidebar-view []
  (let [open? (subscribe [:sidebar-open?])]
    (fn []
      [:div.sidebar {:class (if @open? "open" "closed")
                     :on-click (fn [_]
                                 (dispatch [:close-sidebar!]))}
       [groups-view]])))

(defn main-view []
  [:div.main
   [:div.hamburger
    {:on-click (fn [_]
                 (dispatch [:open-sidebar!]))} "="]
   [sidebar-view]
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
       [:style styles]
       (if @logged-in?
         [main-view]
         [login-view])])))

(defn init []
  (r/render [app-view] (.-body js/document)))

(r/render [app-view] (.-body js/document))
