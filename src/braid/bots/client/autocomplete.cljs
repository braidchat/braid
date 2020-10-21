(ns braid.bots.client.autocomplete
  (:require
   [braid.lib.upload :as upload]
   [braid.core.client.ui.views.autocomplete :refer [fuzzy-matches?]]
   [clojure.string :as string]
   [re-frame.core :refer [subscribe dispatch]]))

; /<bot-name> -> autocompletes bots
(defn bots-autocomplete-engine [text]
  (let [pattern #"^/(\w+)$"
        open-group (subscribe [:open-group-id])]
    (when-let [bot-name (second (re-find pattern text))]
      (into ()
            (comp (filter (fn [b] (fuzzy-matches? (b :nickname) bot-name)))
                  (map (fn [b]
                         {:key (constantly (b :id))
                          :action (fn [])
                          :message-transform
                          (fn [text]
                            (string/replace text pattern
                                            (str "/" (b :nickname) " ")))
                          :html
                          (constantly
                            [:div.bot.match
                             [:img.avatar {:src (upload/->path (b :avatar))}]
                             [:div.info
                              [:div.name (b :nickname)]
                              [:div.extra]]])})))
            @(subscribe [:bots/group-bots] [open-group])))))
