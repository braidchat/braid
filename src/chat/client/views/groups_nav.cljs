(ns chat.client.views.groups-nav
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.views.pills :refer [id->color]]))

(defn groups-nav-view [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "groups"}
        (map (fn [group]
               (dom/div #js {:className "group"
                             :style #js {:backgroundColor (id->color (group :id))}
                             :onClick (fn [e])}
                 (string/join "" (take 2 (group :name)))))
             (vals (data :groups)))))))
