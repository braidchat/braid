(ns braid.bots.client.autocomplete
  (:require
   [braid.core.client.ui.views.autocomplete :refer [fuzzy-matches?]]
   [clojure.string :as string]
   [re-frame.core :refer [subscribe dispatch]]))

; /<bot-name> -> autocompletes bots
(defn bot-autocomplete-engine [text]
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
                             [:img.avatar {:src (b :avatar)}]
                             [:div.info
                              [:div.name (b :nickname)]
                              [:div.extra]]])})))
            @(subscribe [:group-bots] [open-group])))))
