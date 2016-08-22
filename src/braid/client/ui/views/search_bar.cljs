(ns braid.client.ui.views.search-bar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [re-frame.core :refer [dispatch]]
            [braid.client.helpers :refer [debounce ->color]]
            [re-frame.core :refer [subscribe]]
            [braid.client.routes :as routes]))

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
                     (dispatch [:set-search-results [query {}]])
                     (if (string/blank? query)
                       (exit-search!)
                       (do (dispatch [:search-history [query @open-group-id]])
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
                       (dispatch [:set-search-query query])
                       (put! search-chan {:query query})))}]
          (if (seq @search-query)
            [:div.action.clear {:on-click (fn []
                                            (exit-search!))
                                :style {:color (->color @open-group-id)}}]
            [:div.action.search])])})))
