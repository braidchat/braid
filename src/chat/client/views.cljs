(ns chat.client.views
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.user-modal :refer [user-modal-view]]
            [chat.client.views.sidebar :refer [sidebar-view]]
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
          (seq (get-in data [:page :search-result-ids]))
          (apply dom/div #js {:className "threads"}
            (map (fn [t] (om/build thread-view t
                                   {:key :id
                                    :opts {:searched? true}}))
                 ; sort-by last reply, newest first
                 (->> (select-keys (data :threads) (get-in data [:page :search-result-ids]))
                      vals
                      (sort-by
                        (comp (partial apply max)
                              (partial map :created-at)
                              :messages))
                      reverse)))

          ; no results
          :else
          (dom/div #js {:className "title"} "No results"))))))


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
      (println (data :user))
      (dom/div nil
        (if (data :session)
          (om/build main-view data)
          (om/build login-view data))))))
