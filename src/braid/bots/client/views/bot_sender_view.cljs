(ns braid.bots.client.views.bot-sender-view
  (:require
   [braid.lib.color :as color]
   [braid.core.client.routes :as routes]
   [re-frame.core :refer [subscribe]]))

(defn sender-view
  [bot-id]
  (let [sender @(subscribe [:bots/group-bot bot-id])
        current-group (subscribe [:open-group-id])
        sender-path (routes/group-page-path {:group-id @current-group
                                             :page-id "bots"})]
    (when sender
      {:avatar [:a.avatar {:href sender-path
                           :tab-index -1}
                [:img {:src (:avatar sender)
                       :style {:background-color (color/->color (:id sender))}}]]
       :info [:span.bot-notice "BOT"]
       :name [:a.nickname {:tab-index -1
                           :href sender-path}
              (:nickname sender)]})))
