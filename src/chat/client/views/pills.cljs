(ns chat.client.views.pills
  (:require [om.core :as om]
            [om.dom :as dom]
            [braid.ui.views.pills :refer [subscribe-button-view tag-pill-view user-pill-view]]
            [chat.client.reagent-adapter :refer [reagent->react subscribe]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color user-cursor]]
            [chat.client.routes :as routes]))

(defn subscribe-button-view-temp [props]
  [subscribe-button-view (props :tag) subscribe])

(defn tag-pill-view-temp [props]
  [tag-pill-view (props :tag) subscribe])

(defn user-pill-view-temp [props]
  [user-pill-view (props :user) subscribe])

(def SubscribeButtonView
  (reagent->react subscribe-button-view-temp))

(def TagPillView
  (reagent->react tag-pill-view-temp))

(def UserPillView
  (reagent->react user-pill-view-temp))

(defn subscribe-button [tag]
  (reify
    om/IRender
    (render [_]
      (SubscribeButtonView. #js {:tag tag}))))

(defn tag-view [tag]
  (reify
    om/IRender
    (render [_]
      (TagPillView. #js {:tag tag}))))

(defn user-view [user]
  (reify
    om/IRender
    (render [_]
      (UserPillView. #js {:user user}))))
