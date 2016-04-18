(ns braid.ui.views.new-thread
  (:require [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.thread :refer [thread-view]]))

(defn new-thread-view [opts]
  (let [new-thread-id (subscribe [:new-thread-id])]
    (fn [opts]
      [thread-view (merge {:id @new-thread-id
                           :new? true
                           :tag-ids []
                           :mentioned-ids []
                           :messages []}
                          opts)])))
