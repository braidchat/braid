(ns braid.core.client.ui.views.new-message-action-button
  (:require
    [braid.popovers.helpers :as popovers]
    [braid.core.client.ui.views.hover-menu :refer [hover-menu-view]]
    [braid.core.client.ui.views.header-item :refer [HeaderItem]]
    [braid.core.hooks :as hooks]))

(defonce menu-items
  (hooks/register! (atom []) [HeaderItem]))

(defn new-message-action-button-view
  [{:keys [thread-id group-id] :as config}]
  [:div.plus
   (let [popup-view (fn [] [hover-menu-view
                           {:thread-id thread-id
                            :group-id group-id}
                           @menu-items :top])]
     {:on-mouse-enter
      (popovers/on-mouse-enter popup-view)
      :on-touch-start
      (popovers/on-touch-start popup-view)})])
