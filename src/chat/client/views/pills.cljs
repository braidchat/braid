(ns chat.client.views.pills
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color user-cursor]]
            [chat.client.routes :as routes]))

(defn subscribe-button [tag]
  (if (store/is-subscribed-to-tag? (tag :id))
    (dom/a #js {:className "button"
                :onClick (fn [_]
                           (dispatch! :unsubscribe-from-tag (tag :id)))}
      "Unsubscribe")
    (dom/a #js {:className "button"
                :onClick (fn [_]
                           (dispatch! :subscribe-to-tag (tag :id)))}
      "Subscribe")))

(defn tag-view [tag owner]
  (reify
    om/IRender
    (render [_]
      (dom/a #js {:className (str "tag pill"
                                  (if (store/is-subscribed-to-tag? (tag :id))
                                    " on"
                                    " off"))
                  :tabIndex -1
                  :style #js {:backgroundColor (id->color (tag :id))
                              :color (id->color (tag :id))
                              :borderColor (id->color (tag :id))}
                  :href (routes/tag-page-path {:group-id (routes/current-group)
                                               :tag-id (tag :id)})}
        (dom/span #js {:className "name"} "#" (tag :name))))))

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (let [user (om/observe owner (user-cursor (user :id)))]
        (dom/a #js {:className (str "user pill" (case (user :status)
                                                  :online " on"
                                                  " off"))
                    :tabIndex -1
                    :style #js {:backgroundColor (id->color (user :id))
                                :color (id->color (user :id))
                                :borderColor (id->color (user :id))}
                    :href (routes/user-page-path {:group-id (routes/current-group)
                                                  :user-id (user :id)})}
          (dom/span #js {:className "name"} (str "@" (user :nickname)))
          #_(dom/div #js {:className (str "status " ((fnil name "") (user :status)))}))))))
