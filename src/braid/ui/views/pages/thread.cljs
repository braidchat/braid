(ns braid.ui.views.pages.thread
  (:require [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.thread :refer [thread-view]]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn single-thread-view
  []
  (let [page (subscribe [:page])
        group-id (subscribe [:open-group-id])
        thread-id (reaction (first (@page :thread-ids)))
        all-threads (subscribe [:threads])
        thread (reaction (if-let [th (get @all-threads @thread-id)]
                           th
                           (do (dispatch! :load-threads
                                          {:thread-ids [@thread-id]})
                               nil)))]
    (fn []
      [:div.page.single-thread
       [:div.title "Thread"]
       [:div.content
        (if-let [th @thread]
          [thread-view th]
          [:div.loading])]])))
