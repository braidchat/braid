(ns braid.sidebar.api
  (:require
   [braid.sidebar.ui :as ui]))

;; TODO make it so that it can also add styles
(defn register-button!
  [view]
  (swap! ui/sidebar-extra-views conj view))
