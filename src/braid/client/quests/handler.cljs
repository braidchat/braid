(ns braid.client.quests.handler
  (:require [re-frame.core :as rf]))

(defn install-quests-handler! []
  (rf/add-post-event-callback
    :quest-handler
    (fn [event queue]
      (when (not= "quests" (namespace (first event)))
        (println "event callback" (pr-str event))
        (rf/dispatch [:quests/update-handler event])))))
