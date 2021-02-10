(ns braid.page-inbox.styles
  (:require
    [braid.core.client.ui.styles.mixins :as mixins]))

(def new-thread
  [:>.new-thread
   {;; background is set inline to group-color
    :border-radius "50%"
    :border "none"
    :flex-shrink 0
    :color "white"
    :font-size "4em"
    :width "5rem"
    :height "5rem"
    :margin "0 2rem 2rem 1rem"
    :cursor "pointer"}

   [:&:hover
    ;; double invert *trick* keeps the + white
    {:filter "invert(100%) brightness(1.8) invert(100%)"}]])

(def styles
  [:>.page.inbox

   [:>.threads
    new-thread]

   [:>.intro

    [:>button.clear-inbox
     (mixins/outline-button
       {:text-color "#aaa"
        :border-color "#ccc"
        :hover-text-color "#999"
        :hover-border-color "aaa"
        :icon \uf0d0})]]])
