(ns braid.uploads.core
  (:require
    [braid.core.api :as core]
    #?@(:cljs
         [[cljs-uuid-utils.core :as uuid]
          [re-frame.core :refer [subscribe dispatch dispatch-sync]]
          [braid.uploads.s3 :as s3]]
         :clj
         [[braid.uploads.s3 :as s3]
          [braid.core.server.db.user :as user]])))

(defn init! []
  #?(:cljs
     (do
       (core/register-events!
         {::initiate-upload!
          (fn [{db :db} [_ {:keys [group-id thread-id]}]]
            (.. js/document (getElementById "uploader") click)
            {:db (assoc db ::upload-config {:group-id group-id
                                            :thread-id thread-id})})

          ::create-upload!
          (fn [{db :db} [_ url]]
            (let [thread-id (get-in db [::upload-config :thread-id])
                  group-id (get-in db [::upload-config :group-id])]
              {:websocket-send (list [:braid.server/create-upload
                                      {:id (uuid/make-random-squuid)
                                       :url url
                                       :thread-id thread-id
                                       :group-id group-id}])
               :dispatch [:new-message {:content url
                                        :thread-id thread-id
                                        :group-id group-id
                                        :mentioned-user-ids []
                                        :mentioned-tag-ids []}]}))})

       (core/register-root-view!
         (fn []
           [:div.uploads
            [:input {:type "file"
                     :multiple false
                     :id "uploader"
                     :style {:display "none"}
                     :on-change (fn [e]
                                  (s3/upload
                                    (aget (.. e -target -files) 0)
                                    (fn [url]
                                      (dispatch [::create-upload! url])))
                                  ;; clear the value of this input field
                                  ;; so that it can be re-used with the same file if need be
                                  (set! (.. e -target -value) nil))}]]))

       (core/register-new-message-action-menu-item!
         {:body "Add File"
          :icon \uf093
          :priority 1
          :on-click (fn [{:keys [thread-id group-id]}]
                      (fn [_]
                        ;; use dispatch-sync so that browser allows trigger of click
                        (dispatch-sync [::initiate-upload! {:thread-id thread-id
                                                            :group-id group-id}])))}))
     :clj
     (do
       (core/register-private-http-route!
         [:get "/s3-policy"
          (fn [req]
            (println (get-in req [:session :user-id]))
            (if (user/user-id-exists? (get-in req [:session :user-id]))
              (if-let [policy (s3/generate-policy)]
                {:status 200
                 :headers {"Content-Type" "application/edn"}
                 :body (pr-str policy)}
                {:status 500
                 :headers {"Content-Type" "application/edn"}
                 :body (pr-str {:error "No S3 secret for upload"})})
              {:status 403
               :headers {"Content-Type" "application/edn"}
               :body (pr-str {:error "Unauthorized"})}))]))))
