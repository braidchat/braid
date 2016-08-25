(ns braid.client.ui.views.new-thread
  (:require [re-frame.core :refer [subscribe]]
            [braid.client.ui.views.thread :refer [thread-view]]))

(defn new-thread-view []
  (let [temp-thread (subscribe [:temp-thread])
        open-group-id (subscribe [:open-group-id])]
    (fn []
      ^{:key (@temp-thread :id)}
      [thread-view @temp-thread])))
