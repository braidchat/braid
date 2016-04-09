(ns braid.ui.views.search-bar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [<! put! chan]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [debounce]]
            [chat.client.routes :as routes]))

(defn search-bar-view
  [{:keys [subscribe]}]
  (let [open-group-id (subscribe [:open-group-id])
        search-query (subscribe [:search-query])
        search-chan (chan)]
    (r/create-class
      {:component-will-mount
       (fn []
         (let [search (debounce search-chan 500)]
          (go
            (while true
              (let [{:keys [query]} (<! search)]
                (store/set-search-results! {})
                (if (string/blank? query)
                  (do
                    (routes/go-to! (routes/inbox-page-path {:group-id (routes/current-group)})))
                  (do
                    (routes/go-to! (routes/search-page-path {:group-id (routes/current-group)
                                                             :query query}))
                    (dispatch! :search-history [query @open-group-id]))))))))
       :reagent-render
       (fn []
         [:div.search-bar
           [:input {:type "search"
                    :placeholder "Search History"
                    :value @search-query
                    :on-change
                      (fn [e]
                        (let [query (.. e -target -value)]
                          (store/set-search-query! query)
                          (put! search-chan {:query query})))}]])})))

