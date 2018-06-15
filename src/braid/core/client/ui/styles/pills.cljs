(ns braid.core.client.ui.styles.pills
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]))

(defn tag []
  [:.tag
   [:>.pill
    mixins/pill-box]])

(defn user []
  [:.user
   [:>.pill
    mixins/pill-box]])
