(ns braid.ui.views.search-bar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [debounce]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.routes :as routes]
            [chat.client.views.helpers :refer [->color]]))

(defn search-bar-view
  []
  (let [open-group-id (subscribe [:open-group-id])
        search-query (subscribe [:search-query])
        kill-chan (chan)
        search-chan (chan)
        exit-search! (fn []
                       (routes/go-to! (routes/inbox-page-path {:group-id @open-group-id})))]
    (r/create-class
      {:component-will-mount
       (fn []
         (let [search (debounce search-chan 500)]
           (go
             (loop []
               (let [[v ch] (alts! [search kill-chan])]
                 (when (= ch search)
                   (let [{:keys [query]} v]
                     (store/set-search-results! query {})
                     (if (string/blank? query)
                       (exit-search!)
                       (do (dispatch! :search-history [query @open-group-id])
                           (routes/go-to! (routes/search-page-path {:group-id @open-group-id
                                                                    :query query})))))
                   (recur)))))))

       :component-will-unmount
       (fn []
         (put! kill-chan (js/Date.)))

       :reagent-render
       (fn []
         [:div.search-bar
          [:input {:type "text"
                   :placeholder "Search..."
                   :value @search-query
                   :on-change
                   (fn [e]
                     (let [query (.. e -target -value)]
                       (store/set-search-query! query)
                       (put! search-chan {:query query})))}]
          (if (seq @search-query)
            [:div.action.clear {:on-click (fn []
                                            (exit-search!))
                                :style {:color (->color @open-group-id)}}]
            [:div.action.search])])})))
