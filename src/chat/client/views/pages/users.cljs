(ns chat.client.views.pages.users
  (:require [om.core :as om]
            [om.dom :as dom]))

(defn users-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page users"}
        (dom/div #js {:className "title"} "Users")

        (dom/div #js {:className "content"}
          (dom/div #js {:className "description"}
            (dom/p nil "One day, a list of all users in this group will be here.")))))))
