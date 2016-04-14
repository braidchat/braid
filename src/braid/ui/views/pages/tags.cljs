(ns braid.ui.views.pages.tags
  (:require [om.core :as om]
            [om.dom :as dom]
            [reagent.core :as r]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.pills :refer [tag-view subscribe-button]]
            [chat.shared.util :refer [valid-tag-name? valid-nickname?]])
  (:import [goog.events KeyCodes]))

(defn new-tag-view
  [data]
  (let [error (r/atom nil)
        set-error! (fn [err?] (reset! error err?))]
    (fn []
      (:input.new-tag {:class (when error "error")
                        :on-key-up
                        (fn [e]
                          (let [text (.. e -target -value)]
                            (set-error! (not (valid-tag-name? text)))))
                        :onKeyDown
                        (fn [e]
                          (when (= KeyCodes.ENTER e.keyCode)
                            (let [text (.. e -target -value)]
                              (dispatch! :create-tag [text (data :group-id)]))
                            (.preventDefault e)
                            (aset (.. e -target) "value" "")))
                        :placeholder "New Tag"}))))

(defn tag-info-view
  [tag]
  (fn []
    [:div.tag-info
        [:span.count.threads-count
          (tag :threads-count)]
        [:span.count.subscribers-count
          (tag :subscribers-count)]
        [tag-view tag]
        [subscribe-button tag]]))

(defn channels-page-view
  [{:keys [subscribe]}]
  (let [group-id (subscribe [:open-group-id])
          tags (subscribe [:tags])
          sorted-tags (->> @tags
                        vals
                        (filter (fn [t] (= group-id (t :group-id))))
                        (sort-by :threads-count)
                        reverse)
          subscribed-to-tag? (subscribe [:user-subscribed-to-tag])]
    (fn []
      [:div.page.channels
        [:div.title "Tags"]

        [:div.content
          [new-tag-view {:group-id @group-id}]

          (let [subscribed-tags
                (->> @sorted-tags
                     (filter (fn [t] (@subscribed-to-tag? (t :id)))))]
            (when (seq subscribed-tags)
              [:div
                [:h2 "Subscribed"]
                [:div.tags
                 (for [tag subscribed-tags]
                   ^{:key (tag :id)}
                   [tag-info-view tag])]]))

            (let [recommended-tags
                  (->> @sorted-tags
                       ; TODO actually use some interesting logic here
                       (remove (fn [t] (@subscribed-to-tag? (t :id)))))]
              (when (seq recommended-tags)
                [:div
                  [:h2 "Recommended"]
                  [:div.tags
                   (for [tag recommended-tags]
                    ^{:key (tag :id)}
                    [tag-info-view tag])]]))]])))
