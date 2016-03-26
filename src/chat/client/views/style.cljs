(ns chat.client.views.style
  (:require [garden.core :refer [css]]
            [braid.ui.styles.message]
            [braid.ui.styles.body]))

(def style-view
  (fn [_]
    [:style
     {:type "text/css"
      :dangerouslySetInnerHTML
      {:__html (css
                 braid.ui.styles.body/body
                 braid.ui.styles.message/message
                 braid.ui.styles.message/new-message)}}]))

