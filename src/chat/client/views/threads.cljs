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

(defn message-view [message owner opts]
  (reify
    om/IRender
    (render [_]
      (let [sender (get-in @store/app-state [:users (message :user-id)])]
        (dom/div #js {:className (str "message " (when (:collapse? opts) "collapse"))}
          (dom/img #js {:className "avatar" :src (sender :avatar)})
          (dom/div #js {:className "info"}
            (dom/span #js {:className "nickname"} (or (sender :nickname) (sender :email)))
            (dom/span #js {:className "time"} (helpers/format-date (message :created-at))))
          (apply dom/div #js {:className "content"}
            (helpers/format-message (message :content))))))))

(defn tag-view [tag owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "tag " (rand-nth ["subscribed" ""]))
                    :style #js {:backgroundColor (helpers/tag->color tag)}}
        (dom/span #js {:className "name"} (tag :name))))))

(defn user-view [user owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "user"
                    :style #js {:backgroundColor (helpers/tag->color user)}}
        (dom/span #js {:className "name"} (str "@" (or (user :nickname) (user :email))))
        (dom/div #js {:className (str "status " (rand-nth ["online" "away" "offline"]))})))))

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
                   (om/build user-view u)) mentions)
            (map (fn [tag]
                   (om/build tag-view tag)) tags)))))))

(defn thread-view [thread owner {:keys [searched?] :as opts}]
  (let [scroll-to-bottom
        (fn []
          (when-not (thread :new?) ; need this here b/c get-node breaks if no refs???
            (when-let [messages (om/get-node owner "messages")]
              (set! (.-scrollTop messages) (.-scrollHeight messages)))))]
    (reify
      om/IDidMount
      (did-mount [_]
        (scroll-to-bottom))
      om/IDidUpdate
      (did-update [_ _ _]
        (scroll-to-bottom))
      om/IRender
      (render [_]
        (dom/div #js {:className "thread"}
          (dom/div #js {:className "card"}
            (dom/div #js {:className "head"}
              (when-not (or (thread :new?) searched?)
                (dom/div #js {:className "close"
                              :onClick (fn [_]
                                         (dispatch! :hide-thread {:thread-id (thread :id)}))} "×"))
              (om/build thread-tags-view thread))
            (when-not (thread :new?)
              (apply dom/div #js {:className "messages"
                                  :ref "messages"}
                (->> (thread :messages)
                     (sort-by :created-at)
                     (cons nil)
                     (partition 2 1)
                     (map (fn [[prev-message message]]
                            (om/build message-view
                                      message
                                      {:key :id
                                       :opts {:collapse?
                                              (and (= (:user-id message)
                                                      (:user-id prev-message))
                                                (> (* 2 60 1000) ; 2 minutes
                                                   (- (:created-at message)
                                                      (or (:created-at prev-message) 0))))}}))))))
            (om/build new-message-view {:thread-id (thread :id)
                                        :placeholder (if (thread :new?)
                                                       "Start a conversation..."
                                                       "Reply...")}
                      {:react-key "message"})))))))

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



(defn sidebar-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "sidebar"}
        (om/build search-view {})

        (dom/div nil "note: below sections are currently non-functional")
        (dom/h2 nil "Channels")
        (dom/div #js {:className "conversations"}
          (dom/div nil
            (dom/div #js {:className "all"} "ALL")
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
          (->> [{:nickname "foobar"
                 :id (uuid/make-random-squuid)}
                {:nickname "michael"
                 :id (uuid/make-random-squuid)}]
               (map (fn [user]
                      (dom/div nil (om/build user-view user))))))))))

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
        (om/build sidebar-view {})
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
