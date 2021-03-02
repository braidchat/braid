(ns braid.page-inbox.styles
  (:require
    [braid.core.client.ui.styles.threads :refer [>threads]]
    [braid.core.client.ui.styles.mixins :as mixins]))

(defn >new-thread []
  [:>.new-thread
   {;; background is set inline to group-color
    :border-radius "50%"
    :border "none"
    :flex-shrink 0
    :color "white"
    :font-size "4em"
    :width "5rem"
    :height "5rem"
    :cursor "pointer"}

   [:&:hover
    ;; double invert *trick* keeps the + white
    {:filter "invert(100%) brightness(1.8) invert(100%)"}]])

(defn >inbox []
  [:>.inbox
   (>threads)

   [:>.threads
    [:>.sidebar
     {:display "flex"
      :flex-direction "column"
      :align-items "center"
      ;; instead of margins;
      ;; to allow for button to appear without nudging layout
      :width "9rem"
      :margin "0 0 1rem 0"}

     [:>button.resort-inbox
      (mixins/outline-button
        {:text-color "#aaa"
         :border-color "#ccc"
         :hover-text-color "#999"
         :hover-border-color "aaa"})
      {:margin-bottom "1em"}]

     (>new-thread)]]])

(def styles
  [:>.page.inbox
   (>inbox)

   [:>.intro

    [:>button.clear-inbox
     (mixins/outline-button
       {:text-color "#aaa"
        :border-color "#ccc"
        :hover-text-color "#999"
        :hover-border-color "aaa"
        :icon \uf0d0})]]])
