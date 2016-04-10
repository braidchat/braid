(ns chat.client.views.pills
  (:require [om.core :as om]
            [om.dom :as dom]
            [braid.ui.views.pills :refer [tag-pill-view user-pill-view]]
            [chat.client.reagent-adapter :refer [reagent->react subscribe]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color user-cursor]]
            [chat.client.routes :as routes]))

(defn tag-pill-view-temp [props]
  [tag-pill-view (props :tag) subscribe])

(defn user-pill-view-temp [props]
  [user-pill-view (props :user) subscribe])

(def TagPillView
  (reagent->react tag-pill-view-temp))

(def UserPillView
  (reagent->react user-pill-view-temp))

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
      (TagPillView. #js {:tag tag}))))

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (UserPillView. #js {:user user}))))
