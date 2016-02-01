(ns chat.client.views.threads
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.new-message :refer [new-message-view]]
            [chat.client.views.pills :refer [tag-view user-view]]
            [cljs-uuid-utils.core :as uuid]
            [chat.client.emoji :as emoji]
            [chat.client.views.message :refer [message-view]])
  (:import [goog.events KeyCodes]))


(defn thread-tags-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (let [tags (->> (thread :tag-ids)
                      (map #(get-in @store/app-state [:tags %])))
            mentions (thread :mentioned-ids)]
        (apply dom/div #js {:className "tags"}
          (concat
            (map (fn [u-id] (om/build user-view {:id u-id})) mentions)
            (map (fn [tag]
                   (om/build tag-view tag)) tags)))))))

(defn thread-view [thread owner opts]
  (let [scroll-to-bottom
        (fn [owner thread]
          (when-not (thread :new?) ; need this here b/c get-node breaks if no refs???
            (when-let [messages (om/get-node owner "messages")]
              (set! (.-scrollTop messages) (.-scrollHeight messages)))))]
    (reify
      om/IDidMount
      (did-mount [_]
        (scroll-to-bottom owner thread))
      om/IWillReceiveProps
      (will-receive-props [_ _]
        (scroll-to-bottom owner thread))
      om/IDidUpdate
      (did-update [_ _ _]
        (scroll-to-bottom owner thread))
      om/IRender
      (render [_]
        (let [private? (and
                         (not (thread :new?))
                         (empty? (thread :tag-ids))
                         (seq (thread :mentioned-ids)))
              limbo? (and
                       (not (thread :new?))
                       (empty? (thread :tag-ids))
                       (empty? (thread :mentioned-ids)))]
          (dom/div #js {:className (str "thread"
                                        " " (when private? "private")
                                        " " (when limbo? "limbo"))}
            (dom/div #js {:className "card"}
              (dom/div #js {:className "head"}
                (when limbo?
                  (dom/div #js {:className "instructions"}
                    (dom/div nil
                      "No one can see this message yet. Mention a @user or #tag in a reply.")))
                (when (store/open-thread? (thread :id))
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
                                                         "Reply...")
                                          :mentioned-user-ids (thread :mentioned-ids)
                                          :mentioned-tag-ids (thread :tag-ids)}
                        {:react-key "message"}))))))))

(defn new-thread-view [opts]
  (om/build thread-view (merge {:id (uuid/make-random-squuid)
                                :new? true
                                :tag-ids []
                                :mentioned-ids []
                                :messages []}
                               opts)
            {:react-key "new-thread"}))
