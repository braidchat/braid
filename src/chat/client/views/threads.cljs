(ns chat.client.views.threads
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.new-message :refer [new-message-view]]
            [chat.client.views.helpers :as helpers])
  (:import [goog.events KeyCodes]))

(defn message-view [message owner opts]
  (reify
    om/IRender
    (render [_]
      (let [sender (om/observe owner (om/ref-cursor (get-in (om/root-cursor store/app-state) [:users (message :user-id)])))]
        (dom/div #js {:className (str "message " (when (:collapse? opts) "collapse"))}
          (dom/img #js {:className "avatar" :src (sender :avatar)})
          (dom/div #js {:className "info"}
            (dom/span #js {:className "nickname"} (sender :nickname))
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
        (dom/span #js {:className "name"} (str "@" (user :nickname)))
        (dom/div #js {:className (str "status " ((fnil name "") (user :status)))})))))

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
        (fn [owner thread]
          (when-not (thread :new?) ; need this here b/c get-node breaks if no refs???
            (when-let [messages (om/get-node owner "messages")]
              (set! (.-scrollTop messages) (.-scrollHeight messages)))))]
    (reify
      om/IDidMount
      (did-mount [_]
        (scroll-to-bottom owner thread))
      om/IDidUpdate
      (did-update [_ _ _]
        (scroll-to-bottom owner thread))
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

(defn threads-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (apply dom/div #js {:className "threads"}
          (concat
            [(om/build thread-view
                       {:id (uuid/make-random-squuid)
                        :new? true
                        :tag-ids []
                        :messages []}
                       {:react-key "new-thread"})]
            (map (fn [t] (om/build thread-view t {:key :id}))
                 (let [user-id (get-in @store/app-state [:session :user-id])]
                   ; sort by last message sent by logged-in user, most recent first
                   (->> (select-keys (data :threads) (get-in data [:user :open-thread-ids]))
                        vals
                        (sort-by
                          (comp (partial apply max)
                                (partial map :created-at)
                                (partial filter (fn [m] (= (m :user-id) user-id)))
                                :messages))
                        reverse)))))))))
