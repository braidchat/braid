(ns braid.client.ui.views.pages.changelog
  (:require [reagent.core :as r]
            [braid.client.xhr :as xhr]))

(defn changelog-view []
  (let [change-hiccup (r/atom nil)]
    (fn []
      (xhr/edn-xhr {:method :get
                    :uri "/changelog"
                    :on-complete
                    (fn [resp]
                      (reset! change-hiccup
                              (or (:braid/ok resp)
                                  [:div.error "Failed to load changelog"])))})
      [:div.page.changelog
       [:div.title "Changelog"]
       [:div.content
        [:h1 "The Evolution of Braid"]
        (if-let [changes @change-hiccup]
          changes
          [:p "Loading..."])]])))
