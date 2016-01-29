(ns chat.client.views.pills
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]))

(defn id->color [id]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> id
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",70%,35%)")))

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
      (dom/div #js {:className (str "tag pill"
                                      (if (store/is-subscribed-to-tag? (tag :id))
                                        " on"
                                        " off"))
                      :style #js {:backgroundColor (id->color (tag :id))
                                  :color (id->color (tag :id))
                                  :borderColor (id->color (tag :id))}
                      :onClick (fn [e]
                                 (store/set-page! {:type :channel
                                                   :id (tag :id)}))}
          (dom/span #js {:className "name"} "#" (tag :name))))))

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (let [user (om/observe owner (-> (om/root-cursor store/app-state)
                                       (get-in [:users (user :id)])
                                       om/ref-cursor))]
        (dom/div #js {:className (str "user pill" (case (user :status)
                                                    :online " on"
                                                    " off"))
                      :style #js {:backgroundColor (id->color (user :id))
                                  :color (id->color (user :id))
                                  :borderColor (id->color (user :id))}
                      :onClick (fn [e]
                                 (store/set-page! {:type :user
                                                   :id (user :id)}))}
          (dom/span #js {:className "name"} (str "@" (user :nickname)))
          #_(dom/div #js {:className (str "status " ((fnil name "") (user :status)))}))))))
