(ns chat.client.views.sidebar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [cljs.core.async :as async :refer [<! >! put! chan alts! timeout]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.pills :refer [tag-view user-view]]))

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

(defn search-box-view [data owner]
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
                    (store/set-search-searching! false)
                    (store/set-page! {:type :inbox}))
                  (do
                    (store/set-page! {:type :search :search-query query})
                    (store/set-search-searching! true)
                    (dispatch! :search-history query))))))))
    om/IRenderState
    (render-state [_ {:keys [search-chan]}]
      (dom/div #js {:className "search"}
        (dom/input #js {:type "search" :placeholder "Search History"
                        :value (:search-query data)
                        :onChange
                        (fn [e]
                          (let [query (.. e -target -value)]
                            (store/set-search-query! query)
                            (put! search-chan {:query query})))})))))

(defn sidebar-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "sidebar"}
        (om/build search-box-view (data :page))

        (dom/div nil "note: below sections are currently non-functional")
        (dom/h2 #js {:className "inbox"
                     :onClick (fn []
                                (store/set-page! {:type :inbox}))}
          "Inbox"
          (dom/span #js {:className "count"}
            (count (get-in @store/app-state [:user :open-thread-ids]))))
        (dom/h2 nil "Channels")
        (dom/div #js {:className "conversations"}
          (apply dom/div nil
            (->> (@store/app-state :tags)
                 vals
                 (take 5)
                 (map (fn [tag]
                        (dom/div nil (om/build tag-view tag) (str "[" (rand-int 10) "]")))))))

        (dom/h2 nil "Direct Messages")
        (apply dom/div #js {:className "users"}
          (->> (@store/app-state :users)
               vals
               (remove (fn [user] (= (get-in @store/app-state [:session :user-id]) (user :id))))
               (map (fn [user]
                      (dom/div nil
                        (om/build user-view user)
                        (str "[" (rand-int 10) "]"))))))

        (dom/h2 nil "Recommended")
        (apply dom/div #js {:className "recommended"}
          (->> (@store/app-state :tags)
               vals
               (remove (fn [t] (store/is-subscribed-to-tag? (t :id))))
               shuffle
               (take 4)
               (map (fn [tag]
                      (dom/div nil (om/build tag-view tag))))))

        (dom/h2 nil "Online")
        (apply dom/div #js {:className "users"}
          (->> (@store/app-state :users)
               vals
               (filter (fn [user] (= :online (user :status))))
               (remove (fn [user] (= (get-in @store/app-state [:session :user-id]) (user :id))))
               (map (fn [user]
                      (dom/div nil
                        (om/build user-view user)
                        (str "[" (rand-int 10) "]"))))))))))
