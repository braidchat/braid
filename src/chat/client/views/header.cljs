(ns chat.client.views.header
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.search-bar :refer [search-bar-view]]
            [chat.client.views.pills :refer [tag-view user-view]]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.routes :as routes]
            [chat.client.dispatcher :refer [dispatch!]]))

(defn header-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "header"}

        (om/build search-bar-view (data :page))))))
