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
        (dom/h1 nil "Extensions")
        (apply dom/div #js {:className "extensions-list"}
          (map (fn [ext]
                 (dom/div #js {:className "extension"}
                   (str (ext :id) ": " (name (ext :type)))))
               (get-in data [:groups (data :open-group-id) :extensions])))))))
