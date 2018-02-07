(ns braid.core.client.ui.views.pages.thread
  (:require
   [braid.core.client.ui.views.thread :refer [thread-view]]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [reaction]]))

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
          (do (dispatch [:load-threads
                         {:thread-ids [@thread-id]}])
            [:div.loading]))]])))
