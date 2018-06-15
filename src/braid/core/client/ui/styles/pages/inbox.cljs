(ns braid.core.client.ui.styles.pages.inbox
  (:require
    [braid.core.client.ui.styles.mixins :as mixins]))

(def inbox-page
  [:>.page.inbox
   [:>.intro
    [:>button.clear-inbox
     (mixins/outline-button
       {:text-color "#aaa"
        :border-color "#ccc"
        :hover-text-color "#999"
        :hover-border-color "aaa"
        :icon \uf0d0})]]])
