(ns braid.recent-page.core
  "Displays threads that were recently closed."
  (:require
   [braid.base.api :as base]
   [braid.chat.api :as chat]
   #?@(:cljs
       [[re-frame.core :refer [subscribe dispatch]]
        [reagent.ratom :include-macros true :refer-macros [reaction]]
        [braid.core.client.routes :as routes]
        [braid.core.client.ui.views.threads :refer [threads-view]]]
       :clj
       [[braid.chat.db.thread :as thread]])))

(defn init! []
  #?(:clj
     (do
       (base/register-server-message-handlers!
        {:braid.server/load-recent-threads
         (fn [{:keys [?data ?reply-fn user-id]}]
           (when ?reply-fn
             (?reply-fn {:braid/ok (thread/recent-threads
                                    {:group-id ?data
                                     :user-id user-id})})))}))

     :cljs
     (do
       (base/register-events!
        {:braid.recent-page/load-recent-threads
         (fn [_ [_ {:keys [group-id on-error on-complete]}]]
           {:websocket-send
            (list
             [:braid.server/load-recent-threads group-id]
             5000
             (fn [reply]
               (if-let [threads (:braid/ok reply)]
                 (dispatch [:add-threads threads])
                 (cond
                   (= reply :chsk/timeout) (on-error "Timed out")
                   (:braid/error reply) (on-error (:braid/error reply))
                   :else (on-error "Something went wrong")))
               (on-complete (some? (:braid/ok reply)))))})})

       (base/register-subs-raw!
        {:braid.recent-page/recent-threads
         (fn [state _ [group-id]]
           (let [not-open? (reaction
                            (complement (get-in @state [:user :open-thread-ids])))
                 threads (reaction (@state :threads))]
             (reaction
              (doall (filter (fn [{grp :group-id id :id}]
                               (and (= grp group-id) (@not-open? id)))
                             (vals @threads))))))})

       (chat/register-group-page!
        {:key :recent
         :on-load (fn [page]
                    (dispatch [:set-page-loading true])
                    (dispatch [:braid.recent-page/load-recent-threads
                               {:group-id (page :group-id)
                                :on-complete (fn [_]
                                               (dispatch [:set-page-loading false]))
                                :on-error (fn [e]
                                            (dispatch [:set-page-loading false])
                                            (dispatch [:set-page-error true]))}]))
         :view (fn []
                 (let [group-id (subscribe [:open-group-id])
                       threads (subscribe [:braid.recent-page/recent-threads] [group-id])
                       user-id (subscribe [:user-id])
                       page (subscribe [:page])]
                   (fn []
                     (let [sorted-threads
                           (->> @threads
                                ; sort by last message sent by logged-in user, most recent first
                                (sort-by
                                 (comp (partial apply max)
                                       (partial map :created-at)
                                       (partial filter (fn [m] (= (m :user-id) @user-id)))
                                       :messages))
                                reverse)]
                       [:div.page.recent
                        (if (and (not (@page :loading?)) (empty? sorted-threads))
                          [:div.content
                           [:p "No recent threads"]]
                          [threads-view {:threads sorted-threads}])]))))})

       (chat/register-group-header-button!
        {:title "Recently Closed"
         :route-fn routes/group-page-path
         :route-args {:page-id "recent"}
         :icon \uf1da
         :priority 5}))))

