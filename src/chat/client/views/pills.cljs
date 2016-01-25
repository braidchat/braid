(ns chat.client.views.pills
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]))

(defn id->color [id]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> id
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",70%,35%)")))

(defn tag-view [tag owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "tag pill " (rand-nth ["subscribed" ""]))
                    :style #js {:backgroundColor (id->color (tag :id))}
                    :onClick (fn [e]
                               (store/set-page! {:type :channel
                                                 :id (tag :id)}))}
        (dom/span #js {:className "name"} "#" (tag :name))))))

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "user pill "
                    :style #js {:backgroundColor (id->color (user :id))}
                    :onClick (fn [e]
                               (store/set-page! {:type :user
                                                 :id (user :id)}))}
        (dom/span #js {:className "name"} (str "@" (user :nickname)))
        (dom/div #js {:className (str "status " ((fnil name "") (user :status)))})))))
