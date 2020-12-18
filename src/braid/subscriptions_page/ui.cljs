(ns braid.subscriptions-page.ui
  (:require
   [clojure.string :as string]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [reagent.ratom :include-macros true :refer-macros [reaction]]
   [braid.core.client.ui.views.pills :refer [tag-pill-view]]
   [braid.core.client.ui.views.subscribe-button :refer [subscribe-button-view]]
   [braid.core.common.util :refer [valid-tag-name?]])
  (:import
   (goog.events KeyCodes)))

(defn edit-description-view
  [tag]
  (let [editing? (r/atom false)
        new-description (r/atom (or (:description tag) ""))]
    (fn [tag]
      [:span.description-edit
       (if @editing?
         [:div
          [:textarea {:placeholder "New description"
                      :value @new-description
                      :on-change (fn [e]
                                   (reset! new-description (.. e -target -value)))}]
          [:button {:on-click
                    (fn [_]
                      (swap! editing? not)
                      (dispatch [:set-tag-description {:tag-id (tag :id)
                                                       :description @new-description}]))}
           "Save"]
          [:button {:on-click (fn [_]
                                (swap! editing? not)
                                (reset! new-description
                                        (or (:description tag) "")))}
           "Cancel"]]
         [:button {:on-click (fn [_] (swap! editing? not))}
          "Edit description"])])))

(defn new-tag-view
  [data]
  (let [error (r/atom nil)
        set-error! (fn [err?] (reset! error err?))
        text (r/atom "")]
    (fn [data]
      [:div.new-tag
       [:h2 "Create a new tag"]
       [:input
        {:class (when @error "error")
         :value @text
         :on-change (fn [e]
                      (reset! text (.. e -target -value))
                      (when-not (string/blank? @text)
                        (set-error! (not (valid-tag-name? @text)))) )
         :on-key-down
         (fn [e]
           (when (= KeyCodes.ENTER e.keyCode)
             (dispatch [:create-tag {:tag {:name @text
                                           :group-id (data :group-id)}}])
             (.preventDefault e)
             (reset! text "")))
         :placeholder "New Tag"}]
       [:button
        {:on-click (fn [_]
                     (when-not (string/blank? @text)
                       (dispatch [:create-tag {:tag {:name @text
                                                     :group-id (data :group-id)}}])
                       (reset! text "")))
         :disabled (or @error (string/blank? @text))}
        "Create Tag"]])))

(defn delete-tag-view
  [tag]
  [:button.delete {:on-click (fn [_] (dispatch [:remove-tag {:tag-id (tag :id)}]))}
   \uf1f8])

(defn tag-info-view
  [tag]
  (let [group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    [:div.tag-info
     [:span.count.threads-count
      (tag :threads-count)]
     [:span.count.subscribers-count
      (tag :subscribers-count)]
     [:span.tag [tag-pill-view (tag :id)]]
     [subscribe-button-view (tag :id)]
     [:div.description
      [:p
       (if (string/blank? (tag :description))
         "One day, a tag description will be here."
         (tag :description))]
      (when @admin?
        [:div
         [edit-description-view tag]
         [delete-tag-view tag]])]]))

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
      [:div.page.tags
       [:div.title "Manage Tag Subscriptions"]

       [:div.content
        [new-tag-view {:group-id @group-id}]

        (when (seq @subscribed-tags)
          [:div.subscribed.tag-list
           [:h2 "Subscribed"]
           [:div.tags
            (doall
              (for [tag @subscribed-tags]
                ^{:key (tag :id)}
                [tag-info-view tag]))]])

        (when (seq @recommended-tags)
          [:div.recommended.tag-list
           [:h2 "Recommended"]
           [:div.tags
            (doall
              (for [tag @recommended-tags]
                ^{:key (tag :id)}
                [tag-info-view tag]))]])]])))
