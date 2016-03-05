(ns chat.server.extensions.asana
  (:require [clojure.edn :as edn]
            [clojure.data.json :as json]
            [org.httpkit.client :as http :refer [url-encode]]
            [taoensso.carmine :as car]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [chat.server.db :as db]
            [chat.server.cache :refer [cache-set! cache-get cache-del!
                                       random-nonce]]
            [chat.server.extensions :refer [redirect-uri webhook-uri
                                            handle-thread-change handle-webhook
                                            handle-oauth-token extension-config
                                            str->b64 b64->str edn-response]]
            [chat.server.crypto :refer [hmac-verify]]
            [chat.server.sync :as sync]))

(def client-id (env :asana-client-id))
(def client-secret (env :asana-client-secret))

; TODO: make these configurable?
(def comment-format-str "Comment via Braid Chat from %s:\n%s")
(def new-issue-format "New issues from %s:\n%s\n%s")
(def issue-comment-format "New comment from %s:\n%s")

;; Setup

(defn create-asana-extension
  [{:keys [id group-id user-name tag-id]}]
  ; TODO: verify user name is valid
  (db/with-conn
    (let [user-id (db/uuid)]
      ; TODO: need a reasonable way to create this extension-only user
      (db/create-user! {:id user-id
                        :email (random-nonce 50)
                        :password (random-nonce 50)
                        ; TODO: ability to set avatar/reasonable default avatar
                        :avatar "data:image/gif;base64,R0lGODlhAQABAPAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="
                        :nickname user-name})
      (db/user-add-to-group! user-id group-id)
      (db/create-extension! {:id id
                             :group-id group-id
                             :type :asana
                             :user-id user-id
                             :config {:tag-id tag-id}}))))

(declare unregister-webhook)

(defn destroy-asana-extension
  [id]
  (db/with-conn
    (when-let [webhook-id (-> (db/extension-by-id id)
                              (get-in [:config :webhook-id]))]
      (unregister-webhook id webhook-id))
    (db/retract-extension! id)))

;; Authentication flow
(def token-url "https://app.asana.com/-/oauth_token")
(def authorization-url "https://app.asana.com/-/oauth_authorize")
(def api-url "https://app.asana.com/api/1.0")

(defn auth-url
  [extension-id]
  (let [nonce (random-nonce 20)
        info (-> {:extension-id extension-id
                  :nonce nonce}
                 pr-str str->b64)]
    (cache-set! (str extension-id) nonce)
    (str authorization-url
         "?client_id=" (url-encode client-id)
         "&redirect_uri=" (url-encode redirect-uri)
         "&response_type=" "code"
         "&state=" (url-encode info))))

; TODO: can probably factory pretty much all of this except the URL out
(defmethod handle-oauth-token :asana
  [_ state code]
  (let [{ext-id :extension-id sent-nonce :nonce} (-> state b64->str edn/read-string)]
    (when-let [stored-nonce (cache-get (str ext-id))]
      (cache-del! (str ext-id))
      (when (= stored-nonce sent-nonce)
        (let [resp @(http/post token-url
                      {:form-params {"grant_type" "authorization_code"
                                     "client_id" client-id
                                     "client_secret" client-secret
                                     "redirect_uri" redirect-uri
                                     "code" code}})]
          (if (= 200 (:status resp))
            (let [{:strs [access_token refresh_token]} (-> resp :body
                                                           json/read-str)]
              (db/with-conn
                (db/save-extension-token! ext-id {:access-token access_token
                                                  :refresh-token refresh_token})))
            (timbre/warnf "Bad response when exchanging token %s" (:body resp))))))))

(defn refresh-token
  [ext]
  (let [refresh-tok (:refresh-token ext)]
    (let [resp @(http/post token-url
                           {:form-params {"grant_type" "refresh_token"
                                          "client_id" client-id
                                          "client_secret" client-secret
                                          "redirect_uri" redirect-uri
                                          "refresh_token" refresh-tok}})]
      (if (= 200 (:status resp))
        (let [{:strs [access_token refresh_token]} (json/read-str (:body resp))]
          (db/with-conn
            (db/save-extension-token! (ext :id)
                                      {:access-token access_token
                                       :refresh-token (or refresh_token
                                                          refresh-tok)})))
        (do (timbre/warnf "Bad response when exchanging token %s" (:body resp))
            nil)))))

;; Fetching information
(defn fetch-asana-info
  ([ext-id path] (fetch-asana-info ext-id :get path {}))
  ([ext-id method path] (fetch-asana-info ext-id method path {}))
  ([ext-id method path opts]
   (let [ext (db/with-conn (db/extension-by-id ext-id))
         resp @(http/request (merge {:method method
                                     :url (str api-url path)
                                     :oauth-token (ext :token)}
                                    opts))]
     (condp = (:status resp)
       200
       (-> resp :body json/read-str)

       401
       (do (timbre/warnf "token expired %s %s" (:status resp) (:body resp))
           (if (refresh-token ext)
             (fetch-asana-info ext-id path)
             (timbre/warnf "Failed to refresh token")))

       (timbre/warnf "Asana api request failed: %s %s" (:status resp) (:body resp))))))

(defn available-workspaces
  [ext-id]
  (-> (fetch-asana-info ext-id "/users/me")
      (get-in ["data" "workspaces"])))

(defn workspace-projects
  [ext-id workspace-id]
  (-> (fetch-asana-info ext-id (str "/workspaces/" workspace-id "/projects"))
      (get "data")))

;; Webhooks
(defn register-webhook
  [extension-id resource-id]
  (let [{:strs [data]}
        (fetch-asana-info extension-id :post "/webhooks"
                          {:form-params {"resource" resource-id
                                         "target" (str webhook-uri "/" extension-id)}})]
    (db/with-conn
      (db/set-extension-config! extension-id :webhook-id (data "id")))))

(defn unregister-webhook
  [extension-id webhook-id]
  (fetch-asana-info extension-id :delete (str "/webhooks/" webhook-id)))

(defn update-threads-from-event
  [extension data]
  (let [extant-issues (-> extension (get-in [:config :thread->issue] {}) vals set)
        new-issues (into []
                         (filter #(and (= "task" (% "type"))
                                       (= "added" (% "action"))))
                         (data "events"))
        new-comments (into []
                           (filter #(and (= "story" (% "type"))
                                         (= "added" (% "action"))
                                         (extant-issues (% "parent"))))
                           (data "events"))]
    (timbre/debugf "%s new issues" (count new-issues))
    (doseq [{:strs [resource] :as issue} new-issues]
      (let [thread-id (db/uuid)
            task (fetch-asana-info (extension :id) (str "/tasks/" resource))
            thread->issue (get-in extension [:config :thread->issue] {})
            tag-id (get-in extension [:config :tag-id])]
        (if-let [task-data (get task "data")]
          (do (timbre/debugf "adding thread for task %s" task-data)
              (db/with-conn
                (db/create-message! {:thread-id thread-id
                                     :id (db/uuid)
                                     :content (format new-issue-format
                                                      (get-in task-data ["followers" 0 "name"])
                                                      (task-data "name")
                                                      (task-data "notes"))
                                     :user-id (extension :user-id)
                                     :created-at (java.util.Date.)
                                     :mentioned-user-ids ()
                                     :mentioned-tag-ids [tag-id]})
                (db/set-extension-config!
                  (extension :id)
                  :thread->issue (assoc thread->issue thread-id resource))
                (db/extension-subscribe (extension :id) thread-id))
              (sync/broadcast-thread thread-id ()))
          (timbre/warnf "No such task %s" resource))))
    ; TODO: handle changed issue to add a new message to the thread
    (timbre/debugf "%s new comments" (count new-comments))
    (let [issue->thread (->> (get-in extension [:config :thread->issue] {})
                             (into {} (map (fn [[t i]] [i t]))))]
      (doseq [{:strs [resource parent] :as story} new-comments]
        (if-let [thread-id (issue->thread parent)]
          (let [story-data (-> (fetch-asana-info (extension :id) (str "/stories/" resource))
                               (get "data"))]
            (timbre/debugf "new comment %s" story story-data)
            (db/with-conn
              (db/create-message! {:thread-id thread-id
                                   :id (db/uuid)
                                   :content (format issue-comment-format
                                              (get-in story-data ["created_by" "name"])
                                              (story-data "text"))
                                   :user-id (extension :user-id)
                                   :created-at (java.util.Date.)
                                   :mentioned-user-ids ()
                                   :mentioned-tag-ids ()}))
            (sync/broadcast-thread thread-id ()))
          (timbre/warnf "No existing thread for resource %s" resource))))))

(defmethod handle-webhook :asana
  [extension event-req]
  (if-let [secret (get-in event-req [:headers "x-hook-secret"])]
    (do (timbre/debugf "webhook handshake for %s" (extension :id))
        (db/with-conn
          (db/set-extension-config! (extension :id) :webhook-secret secret))
      {:status 200 :headers {"X-Hook-Secret" secret}})
    (if-let [signature (get-in event-req [:headers "x-hook-signature"])]
      (let [body (:body event-req)
            body (if (string? body) body (slurp body))]
        (timbre/debugf "webhook %s" event-req)
        (assert (some? (get-in extension [:config :webhook-secret]))
                "Extension must have a webhook secret before it can recieve data")
        (if (hmac-verify {:secret (get-in extension [:config :webhook-secret])
                          :data body
                          :mac signature})
          (let [data (json/read-str body)]
            (timbre/debugf "event data: %s" data)
            (update-threads-from-event extension data)
            {:status 200})
          (do (timbre/debugf "bad hmac %s for %s" signature body)
              {:status 400 :body "bad hmac"})))
      (do (timbre/warnf "missing signature on webhook %s" event-req)
          {:status 400 :body "missing signature"}))))

;; watched thread notification

(defmethod handle-thread-change :asana
  [extension msg]
  (timbre/debugf "New message %s for extension %s" msg extension)
  (if-let [issue (get-in extension [:config :thread->issue (msg :thread-id)])]
    (let [sender (db/with-conn (db/user-by-id (msg :user-id)))]
      (fetch-asana-info
        (extension :id) :post (str "/tasks/" issue "/stories")
        {:form-params {"text" (format comment-format-str (sender :nickname)
                                      (msg :content))}}))
    (timbre/warnf "No such issue for thread %s" (msg :thread-id))))

;; configuring
(defn projects
  [ext]
  (if (ext :token)
    (edn-response
      {:ok true
       :workspaces
       (doall
         (map (fn [{:strs [id name]}]
                {:id id
                 :name name
                 :projects (workspace-projects (ext :id) id)})
              (available-workspaces (ext :id))))})
    (edn-response {:error "no asana token"} 400)))

(defn select-project
  [ext project-id]
  (db/with-conn
    (db/set-extension-config! (ext :id) :project-id project-id))
  (register-webhook (ext :id) project-id)
  (edn-response {:ok true}))

(defmethod extension-config :asana
  [ext [method args]]
  (case method
    :select-project (select-project ext (first args))
    :get-projects (projects ext)))

