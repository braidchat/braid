(ns chat.client.views.pages.group-explore
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.helpers :refer [id->color]])
  (:import [goog.events KeyCodes]))

(defn group-explore-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page group-explore"}
        (dom/div #js {:className "title"} "Group Explore")
        (dom/div #js {:className "content"}
          (dom/p nil "One day, you'll be able to see various open groups that may be worth exploring here.")

          (dom/h2 nil "Create Group")
          (dom/div #js {:className "new-group"}
            (dom/label nil "New Group"
              (dom/input #js {:placeholder "Group Name"
                              :onKeyDown
                              (fn [e]
                                (when (= KeyCodes.ENTER e.keyCode)
                                  (.preventDefault e)
                                  (let [group-name (.. e -target -value)]
                                    (dispatch! :create-group {:name group-name})
                                    (set! (.. e -target -value) "")))) }))))
        ))))
