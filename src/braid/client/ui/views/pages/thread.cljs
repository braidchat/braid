(ns braid.client.ui.views.pages.thread
  (:require [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [braid.client.state :refer [subscribe]]
            [braid.client.ui.views.thread :refer [thread-view]]
            [braid.client.dispatcher :refer [dispatch!]]))

(defn single-thread-view
  []
  (let [page (subscribe [:page])
        group-id (subscribe [:open-group-id])
        thread-id (reaction (first (@page :thread-ids)))
        thread (subscribe [:thread] [thread-id])]
    (fn []
      [:div.page.single-thread
       [:div.title "Thread"]
       [:div.content
        (if-let [th @thread]
          [thread-view th]
          (do (dispatch! :load-threads
                         {:thread-ids [@thread-id]})
            [:div.loading]))]])))
