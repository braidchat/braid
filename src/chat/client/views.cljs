(ns chat.client.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [cljs.core.async :as async :refer [<! >! put! chan alts! timeout]]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.user-modal :refer [user-modal-view]]
            [chat.client.views.threads :refer [threads-view tag-view user-view thread-view]]))

(defn login-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:email ""
       :password ""
       :error false})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "login"}
        (when (state :error)
          (dom/div #js {:className "error"}
            "Bad credentials, please try again"))
        (dom/input
          #js {:placeholder "Email"
               :type "text"
               :value (state :email)
               :onChange (fn [e] (om/set-state! owner :email (.. e -target -value)))})
        (dom/input
          #js {:placeholder "Password"
               :type "password"
               :value (state :password)
               :onChange (fn [e] (om/set-state! owner :password (.. e -target -value)))})
        (dom/button
          #js {:onClick (fn [e]
                          (dispatch! :auth
                                     {:email (state :email)
                                      :password (state :password)
                                      :on-error
                                      (fn []
                                        (om/set-state! owner :error true))}))}
          "Let's do this!")))))


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

(defn search-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page search"}
        (cond
          ; searching...
          (get-in data [:page :search-searching])
          (dom/div #js {:className "title"} "Searching...")

          ; results
          (seq (get-in data [:page :search-results]))
          (apply dom/div #js {:className "threads"}
            (map (fn [t] (om/build thread-view t
                                   {:key :id
                                    :opts {:searched? true}}))
                 ; sort-by last reply, newest first
                 (->> (vals (get-in data [:page :search-results]))
                      (sort-by
                        (comp (partial apply max)
                              (partial map :created-at)
                              :messages))
                      reverse)))

          ; no results
          :else
          (dom/div #js {:className "title"} "No results"))))))

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
          (->> [{:nickname "foobar"
                 :id (uuid/make-random-squuid)}
                {:nickname "michael"
                 :id (uuid/make-random-squuid)}]
               (map (fn [user]
                      (dom/div nil (om/build user-view user))))))))))

(defn main-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (when-let [err (data :error-msg)]
          (dom/div #js {:className "error-banner"}
            err
            (dom/span #js {:className "close"
                           :onClick (fn [_] (store/clear-error!))} "×")))
        (dom/div #js {:className "instructions"}
          "Tag a conversation with /... Mention tags with #... and users with @... Emoji :shortcode: support too!")
        (om/build user-modal-view data)
        (om/build sidebar-view data)
        (case (get-in data [:page :type])
          :home (om/build threads-view data)
          :search (om/build search-view data))))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (if (data :session)
          (om/build main-view data)
          (om/build login-view data))))))
