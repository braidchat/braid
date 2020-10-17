(ns braid.core.client.ui.views.pages.changelog
  (:require
   [braid.lib.xhr :as xhr]
   [reagent.core :as r]))

(defn changelog-view []
  (let [change-hiccup (r/atom nil)]
    (fn []
      ; FIXME: circumvents re-frame, should use subscribe and dispatch
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
