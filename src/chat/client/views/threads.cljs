(ns chat.client.views.threads
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [<! >! put! chan alts! timeout]]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.user-modal :refer [user-modal-view]]
            [chat.client.views.new-message :refer [new-message-view]]
            [chat.client.views.helpers :as helpers])
  (:import [goog.events KeyCodes]))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (let [sender (get-in @store/app-state [:users (message :user-id)])]
        (dom/div #js {:className "message"}
          (dom/img #js {:className "avatar" :src (sender :avatar)})
          (apply dom/div #js {:className "content"}
            (helpers/format-message (message :content)))
          (dom/div #js {:className "info"}
            (str (or (sender :nickname) (sender :email)) " @ " (helpers/format-date (message :created-at)))))))))

(defn thread-tags-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (let [tags (->> (thread :tag-ids)
                      (map #(get-in @store/app-state [:tags %])))
            mentions (->> (thread :mentioned-ids)
                          (map #(get-in @store/app-state [:users %])))]
        (apply dom/div #js {:className "tags"}
          (concat
            (map (fn [u]
                   (dom/div #js {:className "tag"
                                 :style #js {:backgroundColor (helpers/tag->color u)}}
                     (str "@" (or (u :nickname) (u :email))))) mentions)
            (map (fn [tag]
                   (dom/div #js {:className "tag"
                                 :style #js {:backgroundColor (helpers/tag->color tag)}}
                     (tag :name))) tags)))))))

(defn thread-view [thread owner {:keys [searched?] :as opts}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}

        (dom/div #js {:className "head"}
          (when-not (or (thread :new?) searched?)
            (dom/div #js {:className "close"
                          :onClick (fn [_]
                                     (dispatch! :hide-thread {:thread-id (thread :id)}))} "×"))
          (om/build thread-tags-view thread))
        (when-not (thread :new?)
          (apply dom/div #js {:className "messages"}
            (om/build-all message-view (->> (thread :messages)
                                            (sort-by :created-at))
                          {:key :id})))
        (om/build new-message-view {:thread-id (thread :id)
                                    :placeholder (if (thread :new?)
                                                   "Start a conversation..."
                                                   "Reply...")}
                  {:react-key "message"})))))

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

(defn search-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:search-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [search (debounce (om/get-state owner :search-chan) 1000)]
        (go (while true
              (let [{:keys [query]} (<! search)]
                (if (string/blank? query)
                  (store/set-search-results! {})
                  (dispatch! :search-history query)))))))
    om/IRenderState
    (render-state [_ {:keys [search-chan]}]
      (dom/div #js {:className "search"}
        (dom/input #js {:type "search" :placeholder "Search History"
                        :onChange
                        (fn [e] (put! search-chan {:query (.. e -target -value)}))})))))

(defn threads-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (when-let [err (data :error-msg)]
          (dom/div #js {:className "error-banner"}
            err
            (dom/span #js {:className "close"
                           :onClick (fn [_] (store/clear-error!))}
              "×")))
        (om/build user-modal-view data)
        (om/build search-view {})
        (apply dom/div #js {:className "threads"}
          (concat
            (map (fn [t] (om/build thread-view t
                                   {:key :id
                                    :opts {:searched? (some? (get-in data [:search-results (t :id)]))}}))
                 (->> (vals (merge (data :threads)
                                   (data :search-results)))
                      (sort-by
                        (comp (partial apply min)
                              (partial map :created-at)
                              :messages))))
            [(om/build thread-view
                       {:id (uuid/make-random-squuid)
                        :new? true
                        :tag-ids []
                        :messages []}
                       {:react-key "new-thread"})]))))))
