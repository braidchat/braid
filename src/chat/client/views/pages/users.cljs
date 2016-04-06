(ns chat.client.views.pages.users
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.views.group-invite :refer [group-invite-view]]))

(defn users-page-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page users"}
        (dom/div #js {:className "title"} "Users")

        (dom/h2 nil "Send Invites")
        (dom/div nil
                 (dom/div #js {}
                          (dom/div nil
                                   (:name
                                    (first
                                     (filter #(= (data :open-group-id) (% :id))
                                             (vals (data :groups))))))
                          (om/build group-invite-view (data :open-group-id))))

        (dom/div #js {:className "content"}
          (dom/div #js {:className "description"}
            (dom/p nil "One day, a list of all users in this group will be here.")))))))
