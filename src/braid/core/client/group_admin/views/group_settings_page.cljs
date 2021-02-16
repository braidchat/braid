(ns braid.core.client.group-admin.views.group-settings-page
  (:require
    [reagent.core :as r]
    [reagent.ratom :include-macros true :refer-macros [reaction]]
    [re-frame.core :refer [dispatch subscribe]]
    [braid.lib.upload :as upload]
    [braid.core.hooks :as hooks]
    [braid.core.client.routes :as routes]
    [braid.core.client.ui.views.upload :refer [avatar-upload-view]]))

(defn intro-message-view
  [group]
  (let [new-message (r/atom "")]
    (fn [group]
      [:div.setting.intro
       [:h2 "Intro Message"]
       (if (:intro group)
         [:div [:p "Current intro message"]
          [:blockquote (:intro group)]]
         [:p "No intro message set"])
       [:p "Set new intro"]
       [:textarea {:placeholder "New message"
                   :value @new-message
                   :style {:width "100%"}
                   :on-change (fn [e] (reset! new-message (.. e -target -value)))}]
       [:br]
       [:button {:on-click (fn [_]
                             (dispatch [:set-group-intro! {:group-id (group :id)
                                                          :intro @new-message}])
                             (reset! new-message ""))}
        "Save"]])))

(defn group-avatar-view
  [group]
  (let [dragging? (r/atom false)]
    (fn [group]
      [:div.setting.avatar
       [:h2 "Group Avatar"]
       [:div
        (if (group :avatar)
          [:img {:src (upload/->path (group :avatar))}]
          [:p "Avatar not set"])]
       [:div {:class (when @dragging? "dragging")}
        [avatar-upload-view {:on-upload (fn [url]
                                          (dispatch [:set-group-avatar!
                                                     {:group-id (group :id)
                                                      :avatar url}]))
                             :type "group-avatar"
                             :group-id (group :id)
                             :dragging-change (partial reset! dragging?)}]]])))

(defn publicity-view
  [group]
  [:div.setting.publicity
   [:h2 "Group Publicity Status"]
   (if (group :public?)
     [:div
      [:p "Anyone can join this group by going to "
       [:a {:href (str "/" (group :slug))} "this link"]]
      [:button {:on-click (fn [_]
                            (dispatch [:make-group-private! (group :id)]))}
       "Make private"]]
     [:div
      [:p "This group is private (people can only join if invited)"]
      [:button {:on-click (fn [_]
                            (when (js/confirm "Make this group open to anyone?")
                              (dispatch [:make-group-public! (group :id)])))}
       "Make public"]])])

(defonce extra-group-settings
  (hooks/register! (atom []) [fn?]))

(defn group-settings-page-view
  []
  (let [group (subscribe [:active-group])
        group-id (reaction (:id @group))
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.group-settings
       (when @admin?
         [:div.title (str "Settings for " (:name @group))]
         (into
           [:div.content
            [intro-message-view @group]
            [group-avatar-view @group]
            [publicity-view @group]]
           (for [setting-view @extra-group-settings]
             [setting-view @group])))])))
