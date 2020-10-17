(ns braid.base.client.styles
  (:require
    [braid.core.hooks :as hooks]
    [garden.core :refer [css]]))

(def style-dataspec
  #(or (vector? %) (map? %)))

(defonce module-styles
  (hooks/register!
    (atom [] [style-dataspec])))

(defn styles-view []
  [:style
   {:type "text/css"
    :dangerouslySetInnerHTML
    {:__html (css
               {:auto-prefix #{:transition
                               :flex-direction
                               :flex-shrink
                               :align-items
                               :animation
                               :flex-grow}
                :vendors ["webkit"]}

              @module-styles)}}])
