(ns braid.ui.views.pages.group-settings
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [reagent.core :as r]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.s3 :as s3]
            [chat.client.store :as store]
            [chat.client.routes :as routes]))

(defn leave-group-view
  [group]
  (let [user-id (subscribe [:user-id])]
    (fn [group]
      [:button {:on-click (fn [_]
                            (when (js/confirm "Are you sure you want to leave this group?")
                              (dispatch! :remove-from-group
                                         {:group-id (group :id)
                                          :user-id @user-id})
                              (routes/go-to! (routes/index-path))))}
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
                             (dispatch! :set-intro {:group-id (group :id)
                                                    :intro @new-message})
                             (reset! new-message ""))}
        "Save"]])))

(def max-avatar-size (* 2 1024 1024))

(defn group-avatar-view
  [group]
  (let [uploading? (r/atom false)
        dragging? (r/atom false)
        start-upload (fn [group-id file-list]
                       (let [file (aget file-list 0)]
                         (if (> (.-size file) max-avatar-size)
                           (store/display-error! :avatar-set-fail "Avatar image too large")
                           (do (reset! uploading? true)
                               (s3/upload
                                 file
                                 (fn [url]
                                   (reset! uploading? false)
                                   (dispatch! :set-avatar
                                              {:group-id group-id
                                               :avatar url})))))))]
    (fn [group]
      [:div.setting.avatar {:class (when @dragging? "dragging")}
       [:h2 "Group Avatar"]
       [:div
        (if (group :avatar)
          [:img {:src (group :avatar)}]
          [:p "Avatar not set"]) ]
       [:div.upload
        (if @uploading?
          [:div
           [:p "Uploading..." [:span.uploading-indicator "\uf110"]]]
          [:div
           {:on-drag-over (fn [e]
                            (doto e (.stopPropagation) (.preventDefault))
                            (reset! dragging? true))
            :on-drag-leave (fn [_] (reset! dragging? false))
            :on-drop (fn [e]
                       (.preventDefault e)
                       (reset! dragging? false)
                       (reset! uploading? true)
                       (start-upload (group :id) (.. e -dataTransfer -files)))}
           [:label "Choose a group avatar"
            [:input {:type "file" :accept "image/*"
                     :on-change (fn [e]
                                  (start-upload (group :id)
                                                (.. e -target -files)))}]]])]])))

(defn publicity-view
  [group]
  [:div.setting.publicity
   [:h2 "Group Publicity Status"]
   (if (group :public?)
     [:div
      [:p "Anyone can join this group by going to "
       [:a {:href (str "/group/" (group :name))} "this link"]]
      [:button {:on-click (fn [_]
                            (dispatch! :make-group-private! (group :id)))}
       "Make private"]]
     [:div
      [:p "This group is private (people can only join if invited)"]
      [:button {:on-click (fn [_]
                            (when (js/confirm "Make this group open to anyone?")
                              (dispatch! :make-group-public! (group :id))))}
       "Make public"]])])

(defn group-settings-view
  []
  (let [group (subscribe [:active-group])
        group-id (reaction (:id @group))
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.settings
       [:div.title (str "Settings for " (:name @group))]
       [:div.content
        [leave-group-view @group]
        (when @admin? [intro-message-view @group])
        (when @admin? [group-avatar-view @group])
        (when @admin? [publicity-view @group])]])))
