(ns braid.ui.views.pages.channels
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.pills :refer [tag-view subscribe-button]]
            [chat.shared.util :refer [valid-tag-name? valid-nickname?]])
  (:import [goog.events KeyCodes]))

(defn new-tag-view
  [data]
  (let [error (r/atom nil)
        set-error! (fn [err] (swap! error err))]
    (fn []
      (:input.new-tag {:class (when error "error")
                        :onKeyUp
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
        (om/build tag-view tag)
        (subscribe-button tag)]))

(defn channels-page-view
  [data]
  (fn []
    (let [group-id (data :open-group-id)
          tags (->> (data :tags)
                    vals
                    (filter (fn [t] (= group-id (t :group-id))))
                    (sort-by :threads-count)
                    reverse)]
      [:div.page.channels
        [:div.title "Tags"]

        [:div.content
          [new-tag-view {:group-id group-id}]

          (let [subscribed-tags
                (->> tags
                     (filter (fn [t] (store/is-subscribed-to-tag? (t :id)))))]
            (when (seq subscribed-tags)
              [:div
                [:h2 "Subscribed"]
                (apply dom/div #js {:className "tags"}
                  (map (fn [tag]
                         (om/build tag-info-view tag))
                       subscribed-tags))]))

            (let [recommended-tags
                  (->> tags
                       ; TODO actually use some interesting logic here
                       (remove (fn [t] (store/is-subscribed-to-tag? (t :id)))))]
              (when (seq recommended-tags)
                [:div
                  [:h2 "Recommended"]
                  [:div.tags
                   (for [tag recommended-tags]
                    (om/build tag-info-view tag))]]))]])))