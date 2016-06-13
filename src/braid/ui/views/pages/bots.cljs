(ns braid.ui.views.pages.bots
  (:require [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.pills :refer [user-pill-view]]))

(defn group-bots-view []
  (let [group-id (subscribe [:open-group-id])
        group-bots (subscribe [:group-bots] [group-id])]
    (fn []
      [:div.bots-list
       (doall (for [b @group-bots]
                ^{:key (:id b)}
                [:div.bot
                 [:img.avatar {:src (:avatar b)}]
                 (:nickname b)]))])))

(defn bots-view []
  [:div.page.bots
   [:div.title "Bots"]
   [:div.content
    [group-bots-view]]])
