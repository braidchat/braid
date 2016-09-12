(ns braid.client.group-admin.views.group-settings-page
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [braid.client.s3 :as s3]
            [braid.client.routes :as routes]
            [braid.client.ui.views.upload :refer [avatar-upload-view]]))

(defn leave-group-view
  [group]
  (let [user-id (subscribe [:user-id])]
    (fn [group]
      [:button {:on-click (fn [_]
                            (when (js/confirm "Are you sure you want to leave this group?")
                              (dispatch [:remove-from-group
                                         {:group-id (group :id)
                                          :user-id @user-id}])))}
       "Leave this group"])))

(defn intro-message-view
  [group]
  (let [new-message (r/atom "")]
    (fn [group]
      [:div.setting.intro
       [:h2 "Intro Message"]
       [:p "Current intro message"]
       [:blockquote (:intro group)]
       [:p "Set new intro"]
       [:textarea {:placeholder "New message"
                   :value @new-message
                   :on-change (fn [e] (reset! new-message (.. e -target -value)))}]
       [:button {:on-click (fn [_]
                             (dispatch [:set-group-intro {:group-id (group :id)
                                                          :intro @new-message}])
                             (reset! new-message ""))}
        "Save"]])))

(defn group-avatar-view
  [group]
  (let [dragging? (r/atom false)]
    (fn [group]
      [:div.setting.avatar {:class (when @dragging? "dragging")}
       [:h2 "Group Avatar"]
       [:div
        (if (group :avatar)
          [:img {:src (group :avatar)}]
          [:p "Avatar not set"])]
       [avatar-upload-view {:on-upload (fn [url]
                                         (dispatch [:set-group-avatar
                                                    {:group-id (group :id)
                                                     :avatar url}]))
                            :dragging-change (partial reset! dragging?)}]])))

(defn publicity-view
  [group]
  [:div.setting.publicity
   [:h2 "Group Publicity Status"]
   (if (group :public?)
     [:div
      [:p "Anyone can join this group by going to "
       [:a {:href (str "/group/" (group :name))} "this link"]]
      [:button {:on-click (fn [_]
                            (dispatch [:make-group-private! (group :id)]))}
       "Make private"]]
     [:div
      [:p "This group is private (people can only join if invited)"]
      [:button {:on-click (fn [_]
                            (when (js/confirm "Make this group open to anyone?")
                              (dispatch [:make-group-public! (group :id)])))}
       "Make public"]])])

(defn group-settings-page-view
  []
  (let [group (subscribe [:active-group])
        group-id (reaction (:id @group))
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.group-settings
       [:div.title (str "Settings for " (:name @group))]
       [:div.content
        [leave-group-view @group]
        (when @admin? [intro-message-view @group])
        (when @admin? [group-avatar-view @group])
        (when @admin? [publicity-view @group])]])))
