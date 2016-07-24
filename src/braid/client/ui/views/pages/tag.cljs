(ns braid.client.ui.views.pages.tag
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction run!]]
            [clojure.string :as string]
            [braid.client.store :as store]
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
                      (dispatch! :set-tag-description [(tag :id) @new-description]))}
           "Save"]]
         [:button {:on-click (fn [_] (swap! editing? not))}
          "Edit description"])])))

(defn tag-page-view
  []
  (let [loading? (r/atom false)
        start-loading! (fn [] (reset! loading? true))
        stop-loading! (fn [] (reset! loading? false))
        page-id (subscribe [:page-id])
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
        dummy (run! (when-let [tag-id @page-id]
                      (dispatch! :threads-for-tag {:tag-id tag-id})))
        group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      (let [status (cond
                     @loading? :loading
                     (not (contains? @page :thread-ids)) :searching
                     (seq (@page :thread-ids)) :done-results
                     :else :done-empty)
            _ @dummy]
        [:div.page.channel
         [:div.title
          [tag-pill-view (@tag :id)]
          [subscribe-button-view (@tag :id)]]

         [:div.content
          [:div.description
           [:p
            (if (string/blank? (@tag :description))
              "One day, a tag description will be here."
              (@tag :description))]

           (when @admin?
             [edit-description-view @tag])

           [:div
            (case status
              :searching "Searching..."
              :loading "Loading more..."
              :done-results (str "Done! Displaying " (count @sorted-threads)
                                 " out of " (+ (count @sorted-threads) @pagination-remaining))
              :done-empty "No Results")]]]

         [threads-view
          {:new-thread-args {:tag-ids [(@tag :id)]}
           :threads @sorted-threads
           :threads-opts {:on-scroll ; page in more results as the user scrolls
                          (fn [e]
                            (let [div (.. e -target)]
                              (when (and (= status :done-results)
                                      (> @pagination-remaining 0)
                                      (> 100 (- (.-scrollWidth div)
                                                (+ (.-scrollLeft div) (.-offsetWidth div)))))
                                (start-loading!)
                                (dispatch! :threads-for-tag
                                           {:tag-id @page-id
                                            :offset (count @sorted-threads)
                                            :on-complete
                                            (fn []
                                              (stop-loading!))}))))}}]]))))
