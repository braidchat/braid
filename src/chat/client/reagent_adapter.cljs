(ns chat.client.reagent-adapter
  (:require [reagent.core :as r]))

(defn reagent->react [component]
  (let [ReactComponent (js/React.createFactory
                         (r/reactify-component
                           component))]
    (ReactComponent.)))
