(ns chat.client.views.message
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.views.pills :refer [id->color]]
            [chat.client.views.helpers :as helpers]))

(defn message-view [message owner opts]
  (reify
    om/IRender
    (render [_]
      (let [sender (om/observe owner (helpers/user-cursor (message :user-id)))]
        (dom/div #js {:className (str "message " (when (:collapse? opts) "collapse"))}
          (dom/img #js {:className "avatar"
                        :src (sender :avatar)
                        :style #js {:backgroundColor (id->color (sender :id))}})
          (dom/div #js {:className "info"}
            (dom/span #js {:className "nickname"} (sender :nickname))
            (dom/span #js {:className "time"} (helpers/format-date (message :created-at))))
          (apply dom/div #js {:className "content"}
            (helpers/format-message (message :content))))))))
