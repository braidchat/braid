(ns braid.core.client.ui.styles.body
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]))

(def body
  [:body
   mixins/standard-font
   {:margin 0
    :padding 0
    :background "#F2F2F2"}
   ; prevent overscroll:
   {:height "100%"
    :overflow "hidden"}

   [:textarea :input
    {:font-family "inherit"
     :font-size "1em"}]])
