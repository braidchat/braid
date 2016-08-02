(ns braid.client.ui.views.pages.tag
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction run!]]
            [clojure.string :as string]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.state :refer [subscribe]]
            [braid.client.ui.views.threads :refer [threads-view]]
            [braid.client.ui.views.pills :refer [tag-pill-view subscribe-button-view]]))

(defn edit-description-view
  [tag]
  (let [editing? (r/atom false)
        new-description (r/atom "")]
    (fn [tag]
      [:div.description-edit
       (if @editing?
         [:div
          [:textarea {:placeholder "New description"
                      :value @new-description
                      :on-change (fn [e]
                                   (reset! new-description (.. e -target -value)))}]
          [:button {:on-click
                    (fn [_]
                      (swap! editing? not)
                      (dispatch! :set-tag-description {:tag-id (tag :id)
                                                       :description @new-description}))}
           "Save"]]
         [:button {:on-click (fn [_] (swap! editing? not))}
          "Edit description"])])))

(defn tag-page-view
  []
  (let [page-id (subscribe [:page-id])
        page (subscribe [:page])
        tag (subscribe [:tag] [page-id])
        user-id (subscribe [:user-id])
        threads (subscribe [:threads])
        pagination-remaining (subscribe [:pagination-remaining])
        inbox-thread-ids (subscribe [:open-thread-ids])
        known-threads (reaction
                        (->> @threads
                             vals
                             (filter
                               (fn [t] (contains? (set (t :tag-ids)) (@tag :id))))))
        sorted-threads (reaction
                         (->> (@page :thread-ids)
                              (select-keys @threads)
                              vals
                              (into (set @known-threads))
                              (map (fn [t] (assoc t :open? (contains? @inbox-thread-ids (t :id)))))
                              ; sort-by last reply, newest first
                              (sort-by
                                (comp (partial apply max)
                                      (partial map :created-at)
                                      :messages))
                              reverse))
        group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.channel
       [:div.title
        [tag-pill-view (@tag :id)]
        [subscribe-button-view (@tag :id)]]

       [:div.content
        [:div.description
         [:p
          (@tag :description)]

         (when @admin?
           [edit-description-view @tag])

         (when (and
                 (not (@page :loading?))
                 (not (seq (@page :thread-ids))))
           [:div "No Threads"])]

        [threads-view
         {:new-thread-args {:tag-ids [(@tag :id)]}
          :threads @sorted-threads
          :threads-opts {:on-scroll ; page in more results as the user scrolls
                         (fn [e]
                           (let [div (.. e -target)]
                             (when (and
                                     (not (@page :loading?))
                                     (> @pagination-remaining 0)
                                     (> 100 (- (.-scrollWidth div)
                                               (+ (.-scrollLeft div) (.-offsetWidth div)))))
                               (dispatch! :set-page-loading true)
                               (dispatch! :threads-for-tag
                                          {:tag-id @page-id
                                           :offset (count @sorted-threads)
                                           :on-complete
                                           (fn []
                                             (dispatch! :set-page-loading false))}))))}}]]])))
