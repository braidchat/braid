(ns braid.ui.views.pages.bots
  (:require [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.pills :refer [user-pill-view]]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn bot-view
  [bot]
  (let [group-id (subscribe [:open-group-id])
        admin? (subscribe [:current-user-is-group-admin?] [group-id])
        detailed-info (r/atom nil)]
    (fn [bot]
      [:div.bot
       [:img.avatar {:src (:avatar bot)}]
       (:nickname bot)
       (when @admin?
         [:div
          (if-let [info @detailed-info]
            [:div
             (into [:dl]
                   (mapcat
                     (fn [[k v]]
                       [[:dt (name k)]
                        [:dd v]]))
                   info)
             [:button {:on-click (fn [_] (reset! detailed-info nil))}
              "Hide"]]
            [:button
             {:on-click (fn [_]
                          (dispatch!
                            :get-bot-info
                            {:bot-id (bot :id)
                             :on-complete (fn [info]
                                            (reset! detailed-info info))}))}
             "See bot config"])])])))

(defn group-bots-view []
  (let [group-id (subscribe [:open-group-id])
        group-bots (subscribe [:group-bots] [group-id])]
    (fn []
      [:div.bots-list
       (if (empty? @group-bots)
         [:h2 "No bots in this group"]
         (doall (for [b @group-bots]
                  ^{:key (:id b)}
                  [bot-view b])))])))

(defn bots-view []
  [:div.page.bots
   [:div.title "Bots"]
   [:div.content
    [group-bots-view]]])
