(ns braid.client.reagent-adapter
  (:require [reagent.core :as r]
            [reagent.ratom :include-macros true :refer-macros [reaction]]
            [braid.client.store :as store]
            [braid.client.state :as state]))

(defn subscribe
  ([v] (state/subscription store/app-state v))
  ([v dynv] ; Dynamic subscription
   (let [dyn-vals (reaction (mapv deref dynv))
         sub (reaction (state/subscription store/app-state (into v @dyn-vals)))]
     (reaction @@sub))))
