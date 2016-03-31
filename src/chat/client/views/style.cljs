(ns chat.client.views.style
  (:require [om.core :as om]
            [om.dom :as dom]
            [garden.core :refer [css]]
            [braid.ui.styles.message]
            [braid.ui.styles.body]))

(defn style-view [_ _]
  (reify
    om/IRender
    (render [_]
      (dom/style #js {:type "text/css"
                      :dangerouslySetInnerHTML
                      #js {:__html (css
                                     braid.ui.styles.body/body
                                     braid.ui.styles.message/message
                                     braid.ui.styles.message/new-message)}}))))
