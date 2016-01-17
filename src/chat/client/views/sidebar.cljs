(ns chat.client.views.sidebar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [cljs.core.async :as async :refer [<! >! put! chan alts! timeout]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.threads :refer [threads-view tag-view user-view thread-view]]
            ))

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
                    (store/set-page! {:type :home}))
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
        (dom/h2 nil "Channels")
        (dom/div #js {:className "conversations"}
          (dom/div nil
            (dom/div #js {:className "all"
                          :onClick (fn []
                                     (store/set-page! {:type :home}))} "ALL")
             "[20]")
          (apply dom/div nil
            (->> [{:name "foo"
                   :id (uuid/make-random-squuid)}
                  {:name "bar"
                   :id (uuid/make-random-squuid)}
                  {:name "baz"
                   :id (uuid/make-random-squuid)}]
                 (map (fn [tag]
                        (dom/div nil (om/build tag-view tag) (str "[" (rand-int 10) "]"))))))
          (apply dom/div nil
            (->> [{:nickname "jon"
                   :id (uuid/make-random-squuid)}
                  {:nickname "bob"
                   :id (uuid/make-random-squuid)}]
                 (map (fn [user]
                        (dom/div nil (om/build user-view user) (str "[" (rand-int 10) "]")))))))

        (dom/h2 nil "Recommended")
        (apply dom/div #js {:className "recommended"}
          (->> [{:name "barbaz"
                 :id (uuid/make-random-squuid)}
                {:name "general"
                 :id (uuid/make-random-squuid)}
                {:name "asdf"
                 :id (uuid/make-random-squuid)}]
               (map (fn [tag]
                      (dom/div nil (om/build tag-view tag))))))

        (dom/h2 nil "Members")
        (apply dom/div #js {:className "users"}
          (->> (@store/app-state :users)
               vals
               (filter (fn [user] (= :online (user :status))))
               (remove (fn [user] (= (get-in @store/app-state [:session :user-id]) (user :id))))
               (map (fn [user]
                      (dom/div nil (om/build user-view user))))))))))
