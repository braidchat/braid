(ns chat.client.views.search-bar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [<! put! chan]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [debounce]]))

(defn search-bar-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:search-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [search (debounce (om/get-state owner :search-chan) 500)]
        (go (while true
              (let [{:keys [query]} (<! search)]
                (store/set-search-results! {})
                (if (string/blank? query)
                  (do
                    (store/set-page! {:type :inbox}))
                  (do
                    (store/set-page! {:type :search :search-query query})
                    ; consider moving this dispatch! into search-page-view
                    (dispatch! :search-history [query (@store/app-state :open-group-id)]))))))))
    om/IRenderState
    (render-state [_ {:keys [search-chan]}]
      (dom/div #js {:className "search-bar"}
        (dom/input #js {:type "search" :placeholder "Search History"
                        :value (:search-query data)
                        :onChange
                        (fn [e]
                          (let [query (.. e -target -value)]
                            (store/set-search-query! query)
                            (put! search-chan {:query query})))})))))
