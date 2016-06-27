(ns braid.ui.views.pages.uploads
  (:require [reagent.core :as r]
            [reagent.ratom :refer-macros [run!]]
            [clojure.string :as string]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.embed :refer [embed-view]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.routes :as routes]))

(defn upload-view
  [upload]
  (let [group-id (subscribe [:open-group-id])]
    (fn [upload]
      [:tr.upload
       [:td [embed-view (upload :url)]]
       [:td (js/decodeURIComponent (last (string/split (upload :url) #"/")))]
       [:td [:a {:href (routes/thread-path {:group-id @group-id
                                            :thread-id (upload :thread-id)})}
             (str "Uploaded at " (upload :uploaded-at))]]])))

(defn uploads-view
  []
  (let [group-id (subscribe [:open-group-id])
        uploads (r/atom :initial)
        get-uploads (run! (dispatch! :get-group-uploads
                                     {:group-id @group-id
                                      :on-complete (partial reset! uploads)}))]
    (fn []
      (let [_ @get-uploads]
        [:div.page.uploads
         [:div.title "Uploads"]
         [:div.content
          (cond
            (= :initial @uploads) [:div.loading "Loading..."]
            (empty? @uploads) [:p "No uploads in this group yet"]
            :else
            [:table.uploads
             [:thead
              [:tr [:th ""] [:th ""] [:th ""]]]
             (into [:tbody]
                   (for [upload @uploads]
                     ^{:key (upload :id)}
                     [upload-view upload]))])]]))))
