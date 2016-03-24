(ns braid.mobile.core
  (:require [reagent.core :as r]))

(defn app-view []
  [:div "Hello World"])

(defn init []
  (r/render-component [app-view] (.-body js/document)))
