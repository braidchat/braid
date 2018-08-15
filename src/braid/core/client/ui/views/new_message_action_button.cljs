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
   {:on-mouse-enter (popovers/on-mouse-enter
                      (fn []
                        [hover-menu-view @menu-items :top]))}])
