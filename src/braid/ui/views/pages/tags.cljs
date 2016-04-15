(ns braid.ui.views.pages.tags
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [braid.ui.views.pills :refer [tag-pill-view subscribe-button-view]]
            [chat.shared.util :refer [valid-tag-name? valid-nickname?]])
  (:import [goog.events KeyCodes]))

(defn- new-tag-view
  [data]
  (let [error (r/atom nil)
        set-error! (fn [err?] (reset! error err?))]
    (fn []
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
              (dispatch! :create-tag [text (data :group-id)]))
            (.preventDefault e)
            (aset (.. e -target) "value" "")))
        :placeholder "New Tag"}])))

(defn- tag-info-view
  [tag]
  [:div.tag-info
   [:span.count.threads-count
    (tag :threads-count)]
   [:span.count.subscribers-count
    (tag :subscribers-count)]
   [tag-pill-view (tag :id)]
   [subscribe-button-view (tag :id)]])

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
