(ns braid.uploads.core
  (:require
   [clojure.string :as string]
   [braid.base.api :as base]
   [braid.embeds.api :as embeds]
   [braid.chat.api :as chat]
   [braid.lib.s3 :as s3]
   #?@(:cljs
       [[cljs-uuid-utils.core :as uuid]
        [braid.lib.color :as color]
        [braid.lib.upload :as upload]
        [braid.core.client.ui.styles.mixins :as mixins]
        [re-frame.core :refer [subscribe dispatch dispatch-sync]]]
       :clj
       [[braid.core.common.util :as util]
        [braid.core.server.db :as db]
        [braid.base.conf :refer [config]]
        [braid.chat.db.thread :as thread]
        [braid.chat.db.group :as group]
        [braid.uploads.db :as db.uploads]
        [braid.chat.db.user :as user]]))
  (:import
   #?@(:clj
       [(java.net URLDecoder)])))

#?(:cljs
   (defn image-embed-view
     [url]
     [:a.image.upload
      {:href url
       :target "_blank"
       :rel "noopener noreferrer"}
      [:img {:src url}]]))

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
         (fn [_ [_ {:keys [file type group-id on-complete]}]]
           (let [id (uuid/make-random-squuid)]
             (s3/upload
              {:file file
               :prefix (str group-id "/" type "/" id "/")
               :on-complete (fn [info]
                              (on-complete (assoc info :id id)))}))
           {})

         :braid.uploads/create-upload!
         (fn [{db :db} [_ {:keys [url upload-id thread-id group-id]
                           :or {thread-id (get-in db [::upload-config :thread-id])
                                group-id (get-in db [::upload-config :group-id])}}]]
           {:websocket-send (list [:braid.server/create-upload
                                   {:id upload-id
                                    :url url
                                    :thread-id thread-id
                                    :group-id group-id}])
            :dispatch [:new-message {:content url
                                     :thread-id thread-id
                                     :group-id group-id
                                     :mentioned-user-ids []
                                     :mentioned-tag-ids []}]})})


       (chat/register-message-transform!
        (fn [node]
          (if (and (string? node) (upload/upload-path? node))
            (let [url node]
              [:a.upload {:href (upload/->path url)
                          :title url
                          :style {:background-color (color/url->color url)
                                  :border-color (color/url->color url)}
                          :on-click (fn [e] (.stopPropagation e))
                          :target "_blank"
                                        ; rel to address vuln caused by target=_blank
                                        ; https://www.jitbit.com/alexblog/256-targetblank---the-most-underestimated-vulnerability-ever/
                          :rel "noopener noreferrer"
                          :tab-index -1}
               (last (string/split url #"/"))])
            node)))

       (base/register-styles!
        [:.message
         [:>.content
          [:a.upload
           mixins/pill-box
           {:background "#000000"
            :max-width "inherit !important"}

           ["&::before"
            (mixins/fontawesome \uf15b)
            {:display "inline"
             :margin-right "0.25em"
             :vertical-align "middle"
             :font-size "0.8em"}]]]])

       (embeds/register-embed!
        {:handler
         (fn [{:keys [urls]}]
           (when-let [url (->> urls
                               (filter upload/upload-path?)
                               (some (fn [url]
                                       (re-matches #".*(png|jpg|jpeg|gif)$" url)))
                               first)]
             [image-embed-view (upload/->path url)]))

         :styles
         [:>.image.upload
          [:>img
           {:width "100%"}]]

         :priority 10})

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
                                            {:file
                                             (aget (.. e -target -files) 0)
                                             :group-id @(subscribe [:open-group-id])
                                             :type "upload"
                                             :on-complete
                                             (fn [{:keys [url id]}]
                                               (dispatch [:braid.uploads/create-upload! {:upload-id id
                                                                                         :url url}])
                                               ;; clear the value of this input field
                                               ;; so that it can be re-used with the same file if need be
                                               (set! (.. e -target -value) nil))}]))}]]))

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
                  (s3/delete-file! config path))
                (db/run-txns!
                  (db.uploads/retract-upload-txn (:id upload))))))

          :braid.server/uploads-in-group
          (fn [{:as ev-msg :keys [?data user-id ?reply-fn]}]
            (when ?reply-fn
              (if (group/user-in-group? user-id ?data)
                (?reply-fn {:braid/ok (db.uploads/uploads-in-group ?data)})
                (?reply-fn {:braid/error "Not allowed"}))))})

       (base/register-private-http-route!
         [:get "/upload/*"
          (fn [request]
            ;; TODO check group-id of asset to see if user can access it
            ;; currently, it is possible for a user in another group to access a file
            ;; IF they know its path (which, is behind a UUID, so, non-trivial to guess/enumerate)
            (let [expires-seconds (* 1 24 60 60) ; one day
                  asset-key (URLDecoder/decode (second (re-matches #"^/upload/(.*)" (request :uri))))]
              {:status 302
               :headers {"Cache-Control" (str "private, max-age=" expires-seconds)
                         "Location" (s3/readable-s3-url config expires-seconds asset-key)}}))])

       (base/register-private-http-route!
         [:get "/s3-policy"
          (fn [req]
            ;; TODO check if user is allowed to upload to this group
            (if (user/user-id-exists? (get-in req [:session :user-id]))
              (if-let [policy (s3/generate-s3-upload-policy config {:starts-with ""})]
                {:status 200
                 :headers {"Content-Type" "application/edn"}
                 :body (pr-str policy)}
                {:status 500
                 :headers {"Content-Type" "application/edn"}
                 :body (pr-str {:error "No S3 secret for upload"})})
              {:status 403
               :headers {"Content-Type" "application/edn"}
               :body (pr-str {:error "Unauthorized"})}))]))))
