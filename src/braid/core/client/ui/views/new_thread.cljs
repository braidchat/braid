(ns braid.core.client.ui.views.new-thread
  (:require
   [braid.core.client.ui.views.thread :refer [thread-view]]
   [re-frame.core :refer [subscribe]]))

(defn new-thread-view
  []
  (let [temp-thread @(subscribe [:temp-thread])]
    ^{:key (temp-thread :id)}
    [thread-view temp-thread]))
