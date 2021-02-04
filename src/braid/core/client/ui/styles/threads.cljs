(ns braid.core.client.ui.styles.threads
  (:require
   [braid.core.client.ui.styles.thread]
   [braid.core.client.ui.styles.message]
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]))

(defn >threads []
  [:>.threads
   mixins/flex
   {:position "absolute"
    :top "2.6rem"
    :right 0
    :bottom 0
    :left 0
    :padding-left vars/pad
    :align-items "flex-end"
    :overflow-x "auto" }

   (braid.core.client.ui.styles.thread/thread vars/pad)
   (braid.core.client.ui.styles.thread/notice vars/pad)

   [:>.thread

    [:>.card

     [:>.messages
      braid.core.client.ui.styles.message/message]

     (braid.core.client.ui.styles.thread/new-message vars/pad)]]])
