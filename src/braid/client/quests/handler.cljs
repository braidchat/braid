(ns braid.client.quests.handler
  (:require [re-frame.core :as rf]))

(defn install-quests-handler! []
  (rf/remove-post-event-callback :quest-handler)
  (rf/add-post-event-callback
    :quest-handler
    (fn [event queue]
      (when (not= "quests" (namespace (first event)))
        (rf/dispatch [:quests/update-handler event])))))
