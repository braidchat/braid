(ns chat.client.views.sidebar
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.views.pills :refer [tag-view user-view]]
            ))

(defn sidebar-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "sidebar"}

        (dom/h2 #js {:className "inbox link"
                     :onClick (fn []
                                (store/set-page! {:type :inbox}))}
          "Inbox"
          (dom/span #js {:className "count"}
            (count (get-in @store/app-state [:user :open-thread-ids]))))))))
