(ns braid.rss.core
  "Extension to post updates from RSS feeds as messages in a given group"
  (:require
   [braid.base.api :as base]
   [braid.chat.api :as chat]
   #?@(:clj
       [[braid.rss.server.db :as db]
        [braid.rss.server.fetching :as fetching]
        [braid.chat.db.group :as group-db]
        [taoensso.timbre :as timbre]]
       :cljs
       [[braid.rss.client.views :as views]
        [re-frame.core :refer [dispatch]]])))

(defn init!
  []
  #?(:clj
     (do
       (base/register-db-schema! db/schema)

       (base/register-server-message-handlers!
         {:braid.server.rss/load-feeds
          (fn [{user-id :user-id group-id :?data}]
            (when (group-db/user-in-group? user-id group-id)
              {:reply! {:braid/ok (db/group-feeds group-id)}}))

          :braid.server.rss/check-feed-url
          (fn [{user-id :user-id feed-url :?data}]
            (when user-id
              {:reply! {:braid/ok (fetching/feed-works? feed-url)}}))

          :braid.server.rss/add-feed
          (fn [{user-id :user-id {:keys [tag-ids feed-url group-id] :as new-feed} :?data}]
            (when (and (group-db/user-is-group-admin? user-id group-id)
                     (let [group-tags (into #{} (map :id) (group-db/group-tags group-id))]
                       (every? group-tags tag-ids)))
              (let [new-feed (assoc new-feed
                                    :id (java.util.UUID/randomUUID)
                                    :user-id user-id)]
                {:db-run-txns! (db/add-rss-feed-txn new-feed)
                 :reply! {:braid/ok new-feed}})))

          :braid.server.rss/force-feed-run!
          (fn [{user-id :user-id feed-id :?data}]
            (let [feed (db/feed-by-id feed-id)]
              (when (some->> feed
                      :group-id
                      (group-db/user-is-group-admin? user-id))
                (try
                  (fetching/update-feed! feed)
                  (catch clojure.lang.ExceptionInfo ex
                    (timbre/errorf "Failed to fetch feed %s: %s" feed
                                   (ex-data ex)))))))

          :braid.server.rss/retract-feed
          (fn [{user-id :user-id feed-id :?data}]
            (let [group-id (db/feed-group-id feed-id)]
              (when (group-db/user-is-group-admin? user-id group-id)
                {:db-run-txns! (db/remove-feed-txn feed-id)
                 :reply! :braid/ok})))})

       (base/register-daily-job!
         (fn []
           (timbre/debugf "Running RSS job")
           (doseq [feed (db/all-feeds)]
             (timbre/debugf "Fetching %s" feed)
             (try
               (fetching/update-feed! feed)
               (catch clojure.lang.ExceptionInfo ex
                 (timbre/errorf "Bad feed %s: %s" feed
                                (ex-data ex))))))))
    :cljs
     (do
       (chat/register-group-setting! views/rss-feed-settings-view)
       (base/register-styles! [:.settings.rss-feeds
                               [:.new-rss-feed
                                [:label {:display "block"}]
                                [:.error {:color "red"}]]])
       (base/register-state! {:rss/feeds {}} {:rss/feeds any?})
       (base/register-subs! {:rss/feeds
                             (fn [db [_ group-id]]
                               (get-in db [:rss/feeds (or group-id
                                                          (db :open-group-id))]))})
       (base/register-events!
         {:rss/load-group-feeds
          (fn [_ [_ group-id]]
            {:websocket-send (list [:braid.server.rss/load-feeds group-id]
                                   5000
                                   (fn [reply]
                                     (if-let [resp (:braid/ok reply)]
                                       (dispatch [:rss/-store-feeds group-id resp])
                                       (.error js/console "Failed to load feeds "
                                               (pr-str reply)))))})

          :rss/-store-feeds
          (fn [{db :db} [_ group-id feeds]]
            {:db (assoc-in db [:rss/feeds group-id] (set feeds))})

          :rss/-store-feed
          (fn [{db :db} [_ feed]]
            {:db (update-in db [:rss/feeds (feed :group-id)] (fnil conj #{}) feed)})

          :rss/-remove-feed
          (fn [{db :db} [_ feed]]
            {:db (update-in db [:rss/feeds (feed :group-id)] disj feed)})

          :rss/-check-feed-valid
          (fn [_ [_ feed-url on-complete]]
            {:websocket-send
             (list [:braid.server.rss/check-feed-url feed-url]
                   5000
                   (fn [reply]
                     (if (and (map? reply) (contains? reply :braid/ok))
                       (on-complete (:braid/ok reply))
                       (dispatch [:braid.notices/display!
                                  [::feed-url-check
                                   "Something went wrong validating the feed URL"
                                   :error]]))))})

          :rss/check-feed-valid
          (fn [_ [_ feed-url on-complete]]
            {:dispatch-debounce
             [::check-feed [:rss/-check-feed-valid feed-url on-complete]
              1000]})

          :rss/add-feed
          (fn [_ [_ new-feed]]
            {:websocket-send (list [:braid.server.rss/add-feed new-feed]
                                   5000
                                   (fn [reply]
                                     (if-let [resp (:braid/ok reply)]
                                       (dispatch [:rss/-store-feed resp])
                                       (.error js/console "Error adding feed"
                                               (pr-str reply)))))})
          :rss/retract-feed
          (fn [_ [_ feed]]
            {:websocket-send (list [:braid.server.rss/retract-feed (feed :id)]
                                   5000
                                   (fn [reply]
                                     (when (= :braid/ok reply)
                                       (dispatch [:rss/-remove-feed feed]))))})

          :rss/force-feed-run
          (fn [_ [_ feed-id]]
            {:websocket-send (list [:braid.server.rss/force-feed-run! feed-id])})}))))
