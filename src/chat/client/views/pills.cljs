(ns chat.client.views.pills
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.views.helpers :as helpers]))

(defn tag-view [tag owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "tag " (rand-nth ["subscribed" ""]))
                    :style #js {:backgroundColor (helpers/tag->color tag)}}
        (dom/span #js {:className "name"} (tag :name))))))

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "user"
                    :style #js {:backgroundColor (helpers/tag->color user)}}
        (dom/span #js {:className "name"} (str "@" (user :nickname)))
        (dom/div #js {:className (str "status " ((fnil name "") (user :status)))})))))
