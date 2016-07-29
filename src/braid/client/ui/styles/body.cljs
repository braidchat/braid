(ns braid.client.ui.styles.body)

(def body
  [:body
   {:margin 0
    :padding 0
    :font-family "\"Open Sans\", Helvetica, Arial, sans-serif"
    :font-size "12px"
    :background "#F2F2F2"}
   ; prevent overscroll:
   {:height "100%"
    :overflow "hidden"}
   [:textarea :input
    {:font-family "inherit"
     :font-size "1em"}]])
