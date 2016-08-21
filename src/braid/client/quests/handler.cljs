(ns braid.client.quests.handler
  (:require [re-frame.core :as rf]))

(defn install-quests-handler []
  (rf/add-post-event-callback
    (fn [event queue]
      (when (not= (first event) :quests/update-handler)
        (println "event callback" (pr-str event))
        (rf/dispatch [:quests/update-handler event])))))
