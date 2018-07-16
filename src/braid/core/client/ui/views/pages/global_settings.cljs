(ns braid.core.client.ui.views.pages.global-settings
  (:require
   [braid.core.client.desktop.notify :as notify]
   [braid.core.client.store :as store]
   [braid.core.client.ui.views.pills :refer [tag-pill-view]]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [reaction]])
  (:import
   (goog.events KeyCodes)))


(defn email-settings-view
  []
  (let [email-freq (subscribe [:user-preference :email-frequency])]
    (fn []
      [:div.setting
       [:h2 "Email Digest Preferences"]
       [:div
        [:p "If you wish, you can recieve emails notifying you of threads that you"
         " haven't seen yet."]
        [:p (str "Currently getting emails " ((fnil name "never") @email-freq))]
        [:p "Recieve emails: "]
        [:select {:value (or @email-freq :never)
                  :on-change (fn [e]
                               (let [v (keyword (.. e -target -value))]
                                 (dispatch [:set-preference [:email-frequency v]])))}
         (doall
           (for [freq [:never :weekly :daily]]
             ^{:key freq}
             [:option {:value freq} (name freq)]))]]])))

(defn notification-rule-view
  [[event condition]]
  (let [group (if (= condition :any)
                (r/atom {:name "any group"})
                (subscribe [:group condition]))]
    (fn [[event condition]]
      (case event
        [:tr
         [:td (case event
                :any "Any Event"
                :mention "I am mentioned"
                :tag "Message is tagged ")]
         [:td (case event
                (:any :mention)
                (str "In " (:name @group))
                :tag [tag-pill-view condition])]
         [:td [:button {:on-click (fn [_]
                                    (dispatch [:remove-notification-rule
                                               [event condition]]))}
               "-"]]]))))

(defn new-rule-view
  []
  (let [event (r/atom :any)
        condition (r/atom :any)
        event-tag (r/atom nil)
        groups (subscribe [:groups])
        all-tags (subscribe [:tags])
        tags (reaction (group-by :group-id @all-tags))
        default-condition (reaction (case @event
                                      (:any :mention) :any
                                      :tag (:id (first @all-tags))))]
    (fn []
      [:tr
       [:td [:select {:value @event
                      :on-change (fn [e]
                                   (reset! event (keyword (.. e -target -value)))
                                   (reset! condition @default-condition))}
             [:option {:value :any} "Any Event"]
             [:option {:value :mention} "I am mentioned"]
             [:option {:value :tag} "A message is tagged with..."]]]
       [:td
        (case @event
          (:any :mention)
          [:select {:value @condition
                    :on-change (fn [e]
                                 (let [v (.. e -target -value)]
                                   (if (= v "any")
                                     (reset! condition :any)
                                     (reset! condition (uuid v)))))}
           [:option {:value :any} "Any Group"]
           (doall
             (for [group @groups]
               ^{:key (group :id)}
               [:option {:value (group :id)} (group :name)]))]

          :tag
          [:select {:value @event-tag
                    :on-change (fn [e]
                                 (let [tag-id (uuid (.. e -target -value))]
                                   (reset! condition tag-id)))}
           (doall
             (for [group-id (keys @tags)]
               ^{:key group-id}
               [:optgroup {:label (:name @(subscribe [:group group-id]))}
                (doall
                  (for [tag (get @tags group-id)]
                    ^{:key (tag :id)}
                    [:option {:value (tag :id)} (tag :name)]))]))])]
       [:td
        [:button {:on-click (fn [_]
                              (dispatch [:add-notification-rule [@event @condition]]))}
         "Save"]]])))

(defn notification-rules-view
  []
  [:div
   [:table
    [:thead
     [:tr [:th "Notify me if"] [:th "In"] [:th ""]]]
    [:tbody
     (doall
       (for [rule @(subscribe [:user-preference :notification-rules])]
         ^{:key (hash rule)}
         [notification-rule-view rule]))
     [new-rule-view]]]])

(defn notification-settings-view
  []
  (let [notifications-enabled (r/atom (notify/enabled?))]
    (fn []
      [:div.setting
       [:h2 "Notification Preferences"]
       [:div
        [:p "Braid is designed to let you work asynchronously, but sometimes you "
         "want to know right away when something happens.  You can choose events "
         "that will trigger HTML notifications if you're online or emails if offline."]
        (if (notify/has-notify?)
          [:div
           (when-not @notifications-enabled
             [:button
              {:on-click
               (fn [_] (notify/request-permission
                        (fn [perm]
                          (when (= perm "granted")
                            (reset! notifications-enabled true)
                            (notify/notify {:msg "Notifications Enabled!"})))))}
              "Enable Desktop Notifications"])
           [notification-rules-view]]
          [:p "The browser you're currently using doesn't support notifications. "
           "You can enable them here for other platforms and emails though."])]])))

(defn global-settings-page-view
  []
  (fn []
    [:div.page.global-settings
     [:div.title "Global Settings"]
     [:div.content
      [email-settings-view]
      [notification-settings-view]]]))
