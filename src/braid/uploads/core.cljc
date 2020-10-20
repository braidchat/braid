(ns braid.uploads.core
  (:require
    [braid.base.api :as base]
    [braid.chat.api :as chat]
    [braid.lib.s3 :as s3]
    #?@(:cljs
         [[cljs-uuid-utils.core :as uuid]
          [re-frame.core :refer [subscribe dispatch dispatch-sync]]]
         :clj
         [[braid.core.common.util :as util]
          [braid.core.server.db :as db]
          [braid.chat.db.thread :as thread]
          [braid.chat.db.group :as group]
          [braid.uploads.db :as db.uploads]
          [braid.chat.db.user :as user]])))

(def Upload
  {:id uuid?
   :thread-id uuid?
   :uploader-id uuid?
   :uploaded-at inst?
   :url string?})

(defn init! []
  #?(:cljs
     (do
       (base/register-events!
         {::initiate-upload!
          (fn [{db :db} [_ {:keys [group-id thread-id]}]]
            (.. js/document (getElementById "uploader") click)
            {:db (assoc db ::upload-config {:group-id group-id
                                            :thread-id thread-id})})

          :braid.uploads/upload!
          (fn [{db :db} [_ file on-complete]]
            (s3/upload
              file
              on-complete)
            {})

          :braid.uploads/create-upload!
          (fn [{db :db} [_ {:keys [url thread-id group-id]
                           :or {thread-id (get-in db [::upload-config :thread-id])
                                group-id (get-in db [::upload-config :group-id])}}]]
            {:websocket-send (list [:braid.server/create-upload
                                    {:id (uuid/make-random-squuid)
                                     :url url
                                     :thread-id thread-id
                                     :group-id group-id}])
             :dispatch [:new-message {:content url
                                      :thread-id thread-id
                                      :group-id group-id
                                      :mentioned-user-ids []
                                      :mentioned-tag-ids []}]})})

       (base/register-root-view!
         (fn []
           [:div.uploads
            [:input {:type "file"
                     :multiple false
                     :id "uploader"
                     :style {:display "none"}
                     :on-change (fn [e]
                                  (.persist e) ; react dom thing
                                  (dispatch [:braid.uploads/upload!
                                             (aget (.. e -target -files) 0)
                                             (fn [url]
                                               (dispatch [:braid.uploads/create-upload! {:url url}])
                                               ;; clear the value of this input field
                                               ;; so that it can be re-used with the same file if need be
                                               (set! (.. e -target -value) nil))]))}]]))

       (chat/register-new-message-action-menu-item!
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

       (base/register-db-schema!
         [{:db/ident :upload/id
           :db/valueType :db.type/uuid
           :db/cardinality :db.cardinality/one
           :db/unique :db.unique/identity}
          {:db/ident :upload/thread
           :db/doc "The thread this upload is associated with"
           :db/valueType :db.type/ref
           :db/cardinality :db.cardinality/one}
          {:db/ident :upload/url
           :db/valueType :db.type/string
           :db/cardinality :db.cardinality/one }
          {:db/ident :upload/uploaded-at
           :db/valueType :db.type/instant
           :db/cardinality :db.cardinality/one}
          {:db/ident :upload/uploaded-by
           :db/doc "User that uploaded this file"
           :db/valueType :db.type/ref
           :db/cardinality :db.cardinality/one}])

       (base/register-server-message-handlers!
         {:braid.server/create-upload
          (fn [{:as ev-msg :keys [?data user-id]}]
            (let [upload (assoc ?data
                           :uploaded-at (java.util.Date.)
                           :uploader-id user-id)]
              (when (and (util/valid? Upload upload)
                      (let [thread-group-id (thread/thread-group-id (upload :thread-id))]
                        (or (nil? thread-group-id) (= thread-group-id (upload :group-id))))
                      (group/user-in-group? user-id (upload :group-id)))
                (db/run-txns! (db.uploads/create-upload-txn upload)))))

          :braid.server/delete-upload
          (fn [{:as ev-msg :keys [?data user-id ?reply-fn]}]
            (let [upload (db.uploads/upload-info ?data)]
              (when (or (= user-id (:user-id upload))
                        (group/user-is-group-admin? user-id (:group-id upload)))
                (when-let [path (s3/upload-url-path (:url upload))]
                  (s3/delete-upload path))
                (db/run-txns!
                  (db.uploads/retract-upload-txn (:id upload))))))

          :braid.server/uploads-in-group
          (fn [{:as ev-msg :keys [?data user-id ?reply-fn]}]
            (when ?reply-fn
              (if (group/user-in-group? user-id ?data)
                (?reply-fn {:braid/ok (db.uploads/uploads-in-group ?data)})
                (?reply-fn {:braid/error "Not allowed"}))))})

       (base/register-private-http-route!
         [:get "/s3-policy"
          (fn [req]
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
