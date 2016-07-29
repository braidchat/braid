(ns braid.client.ui.views.pages.tags
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [clojure.string :as string]
            [braid.client.state :refer [subscribe]]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.ui.views.pills :refer [tag-pill-view subscribe-button-view]]
            [braid.client.ui.views.pages.tag :refer [edit-description-view]]
            [braid.common.util :refer [valid-tag-name?]])
  (:import [goog.events KeyCodes]))

(defn new-tag-view
  [data]
  (let [error (r/atom nil)
        set-error! (fn [err?] (reset! error err?))]
    (fn [data]
      [:input.new-tag
       {:class (when error "error")
        :on-key-up
        (fn [e]
          (let [text (.. e -target -value)]
            (set-error! (not (valid-tag-name? text)))))
        :on-key-down
        (fn [e]
          (when (= KeyCodes.ENTER e.keyCode)
            (let [text (.. e -target -value)]
              (dispatch! :create-tag {:tag {:name text
                                            :group-id (data :group-id)}}))
            (.preventDefault e)
            (aset (.. e -target) "value" "")))
        :placeholder "New Tag"}])))

(defn delete-tag-view
  [tag]
  [:button {:on-click (fn [_] (dispatch! :retract-tag (tag :id)))}
   "Delete Tag"])

(defn tag-info-view
  [tag]
  (let [group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn [tag]
      [:div.tag-info
       [:span.count.threads-count
        (tag :threads-count)]
       [:span.count.subscribers-count
        (tag :subscribers-count)]
       [tag-pill-view (tag :id)]
       [subscribe-button-view (tag :id)]
       [:div.description
        [:p
         (if (string/blank? (tag :description))
           "One day, a tag description will be here."
           (tag :description))]
        (when @admin?
          [:div
           [edit-description-view tag]
           [delete-tag-view tag]])]])))

(defn tags-page-view
  []
  (let [group-id (subscribe [:open-group-id])
        tags (subscribe [:tags])
        sorted-tags (reaction (->> @tags
                                   (filter (fn [t] (= @group-id (t :group-id))))
                                   (sort-by :threads-count)
                                   reverse))
        subscribed-tag-ids (subscribe [:user-subscribed-tag-ids])
        subscribed-to? (fn [tag-id] (contains? (set @subscribed-tag-ids) tag-id))
        subscribed-tags (reaction
                          (->> @sorted-tags
                               (filter (fn [t] (subscribed-to? (t :id))))))
        recommended-tags (reaction
                           (->> @sorted-tags
                                ; TODO actually use some interesting logic here
                                (remove (fn [t] (subscribed-to? (t :id))))))]
    (fn []
      [:div.page.channels
       [:div.title "Tags"]

       [:div.content
        [new-tag-view {:group-id @group-id}]

        (when (seq @subscribed-tags)
          [:div
           [:h2 "Subscribed"]
           [:div.tags
            (doall
              (for [tag @subscribed-tags]
                ^{:key (tag :id)}
                [tag-info-view tag]))]])

        (when (seq @recommended-tags)
          [:div
           [:h2 "Recommended"]
           [:div.tags
            (doall
              (for [tag @recommended-tags]
                ^{:key (tag :id)}
                [tag-info-view tag]))]])]])))
