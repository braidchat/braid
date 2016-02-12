(ns chat.client.views.pages.help
  (:require [om.core :as om]
            [om.dom :as dom]))

(defn help-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page help"}
        (dom/div #js {:className "title"} "Help")

        (dom/div #js {:className "content"}
          (dom/div #js {:className "description"}
            (dom/p nil "One day, a help page will be here.")))))))
