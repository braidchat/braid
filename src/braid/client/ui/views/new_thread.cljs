(ns braid.client.ui.views.new-thread
  (:require [re-frame.core :refer [subscribe]]
            [braid.client.ui.views.thread :refer [thread-view]]))

(defn new-thread-view [opts]
  (let [new-thread-id (subscribe [:new-thread-id])
        open-group-id (subscribe [:open-group-id])]
    (fn [opts]
      ^{:key @new-thread-id}
      [thread-view (merge {:id @new-thread-id
                           :group-id (get opts :group-id @open-group-id)
                           :new? true
                           :tag-ids (get opts :tag-ids [])
                           :mentioned-ids (get opts :mentioned-ids [])
                           :messages []}
                          opts)])))
