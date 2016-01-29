(ns chat.client.views.search-bar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [cljs.core.async :as async :refer [<! >! put! chan alts! timeout]]))

(defn debounce
  "Given the input channel source and a debouncing time of msecs, return a new
  channel that will forward the latest event from source at most every msecs
  milliseconds"
  [source msecs]
  (let [out (chan)]
    (go
      (loop [state ::init
             lastv nil
             chans [source]]
        (let [[_ threshold] chans]
          (let [[v sc] (alts! chans)]
            (condp = sc
              source (recur ::debouncing v
                            (case state
                              ::init (conj chans (timeout msecs))
                              ::debouncing (conj (pop chans) (timeout msecs))))
              threshold (do (when lastv
                              (put! out lastv))
                            (recur ::init nil (pop chans))))))))
    out))

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
                    (dispatch! :search-history query))))))))
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
