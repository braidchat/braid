(ns braid.core.client.ui.views.mentions
  (:require
    [braid.popovers.helpers :as popover]
    [braid.core.client.ui.views.pills :refer [user-pill-view tag-pill-view]]
    [braid.core.client.ui.views.user-hover-card :refer [user-hover-card-view]]
    [braid.core.client.ui.views.tag-hover-card :refer [tag-hover-card-view]]))

(defn user-mention-view
  [user-id]
  [:div.user
   {:style {:display "inline-block"}
    :on-mouse-enter
    (popover/on-mouse-enter
      (fn []
        [user-hover-card-view user-id]))}
   [user-pill-view user-id]])

(defn tag-mention-view
  [tag-id]
  [:div.tag
   {:style {:display "inline-block"}
    :on-mouse-enter
    (popover/on-mouse-enter
      (fn []
        [tag-hover-card-view tag-id]))}
   [tag-pill-view tag-id]])
