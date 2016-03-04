(ns chat.client.views.pages.extensions
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]))

(defn extensions-page-view
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page extensions"}
        (dom/h1 nil "Extensions")))))
