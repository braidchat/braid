(ns chat.server.routes
  (:require
    [clojure.string :as string]
    [clojure.edn :as edn]
    [compojure.core :refer [GET POST defroutes context]]
    [compojure.route :refer [resources]]
    [compojure.coercions :refer [as-uuid]]
    [clostache.parser :as clostache]
    [chat.shared.util :refer [valid-nickname?]]
    [chat.server.digest :as digest]
    [chat.server.db :as db]
    [chat.server.invite :as invites]
    [chat.server.identicons :as identicons]
    [chat.server.cache :refer [random-nonce]]
    [chat.server.sync :as sync]
    [chat.server.extensions :as ext :refer [b64->str]]
    [chat.server.s3 :as s3]
    [chat.server.conf :refer [api-port]]
    [braid.api.embedly :as embedly]
    [environ.core :refer [env]]))

(defn- edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})

(defn- get-html [client]
  (clostache/render-resource
    (str "public/" client ".html")
    {:algo "sha256"
     :js (str (digest/from-file (str "public/js/" client "/out/braid.js")))
     :api_domain (or (:api-domain env) (str "localhost:" @api-port))}))

(defroutes desktop-client-routes
  ; public group page
  (GET "/group/:group-name" [group-name :as req]
    (if-let [group (db/with-conn (db/public-group-with-name group-name))]
      (clostache/render-resource "public/public_group_desktop.html.mustache"
                                 {:group-name (group :name)
                                  :group-id (group :id)
                                  :api_domain (or (:api-domain env) (str "localhost:" @api-port))})
      {:status 403
       :headers {"Content-Type" "text/plain"}
       :body "No such public group"}))

  ; invite accept page
  (GET "/accept" [invite :<< as-uuid tok]
    (if (and invite tok)
      (if-let [invite (db/with-conn (db/get-invite invite))]
        {:status 200 :headers {"Content-Type" "text/html"} :body (invites/register-page invite tok)}
        {:status 400 :headers {"Content-Type" "text/plain"} :body "Invalid invite"})
      {:status 400 :headers {"Content-Type" "text/plain"} :body "Bad invite link, sorry"}))

  ; password reset page
  (GET "/reset" [user :<< as-uuid token :as req]
    (if-let [u (and user token
                 (invites/verify-reset-nonce {:id user} token)
                 (db/with-conn (db/user-by-id user)))]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (invites/reset-page u token)}
      {:status 401
       :headers {"Content-Type" "text/plain"}
       :body "Bad user or token"}))

  ; everything else
  (GET "/*" []
    (get-html "desktop")))

(defroutes mobile-client-routes
  ; TODO: add mobile routse for public joining & password resets
  (GET "/*" []
    (get-html "mobile")))

(defroutes api-public-routes
  ; check if already logged in
  (GET "/check" req
    (if-let [user-id (get-in req [:session :user-id])]
      (if-let [user (db/with-conn (db/user-id-exists? user-id))]
        {:status 200 :body ""}
        {:status 401 :body "" :session nil})
      {:status 401 :body "" :session nil}))

  ; log in
  (POST "/auth" req
    (if-let [user-id (let [{:keys [email password]} (req :params)]
                       (when (and email password)
                         (db/with-conn (db/authenticate-user email password))))]
      {:status 200 :session (assoc (req :session) :user-id user-id)}
      {:status 401 :body (pr-str {:error true})}))
  ; log out
  (POST "/logout" req
    {:status 200 :session nil})

  ; request a password reset
  (POST "/request-reset" [email]
    (when-let [user (db/with-conn (db/user-with-email email))]
      (invites/request-reset (assoc user :email email)))
    {:status 200 :body (pr-str {:ok true})})

  ; accept an email invite to join a group
  (POST "/register" [token invite_id password email now hmac nickname avatar :as req]
    (let [fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? password) (assoc fail :body "Must provide a password")

        (not (invites/verify-hmac hmac (str now token invite_id email)))
        (assoc fail :body "Invalid HMAC")

        (string/blank? invite_id) (assoc fail :body "Invalid invitation ID")

        (not (valid-nickname? nickname))
        (assoc fail :body "Nickname must be 1-30 characters without whitespace")

        (db/with-conn (db/nickname-taken? nickname))
        (assoc fail :body "nickname taken")

        ; TODO: be smarter about this
        (not (#{"image/jpeg" "image/png"} (:content-type avatar)))
        (assoc fail :body "Invalid image")

        :else
        (let [invite (db/with-conn (db/get-invite (java.util.UUID/fromString invite_id)))]
          (if-let [err (:error (invites/verify-invite-nonce invite token))]
            (assoc fail :body "Invalid invite token")
            (let [avatar-url (invites/upload-avatar avatar)
                  user (db/with-conn (db/create-user! {:id (db/uuid)
                                                       :email email
                                                       :avatar avatar-url
                                                       :nickname nickname
                                                       :password password}))
                  referer (get-in req [:headers "referer"] (env :site-url))
                  [proto _ referrer-domain] (string/split referer #"/")]
              (db/with-conn
                (db/user-add-to-group! (user :id) (invite :group-id))
                (db/user-subscribe-to-group-tags! (user :id) (invite :group-id))
                (db/retract-invitation! (invite :id)))
              (sync/broadcast-user-change (user :id) [:chat/new-user user])
              {:status 302 :headers {"Location" (str proto "//" referrer-domain)}
               :session (assoc (req :session) :user-id (user :id))
               :body ""}))))))


  ; join a public group
  (POST "/public-register" [group-id email :as req]
    (let [bad-resp {:status 400 :headers {"Content-Type" "text/plain"}}]
      (if-not group-id
        (assoc bad-resp :body "Missing group id")
        (let [group-id (java.util.UUID/fromString group-id)
              group-settings (db/with-conn (db/group-settings group-id))]
          (if-not (get group-settings :public?)
            (assoc bad-resp :body "No such group or the group is private")
            (if (string/blank? email)
              (assoc bad-resp :body "Invalid email")
              (if (db/with-conn (db/user-with-email email))
                (assoc bad-resp
                  :body (str "A user is already registered with that email.\n"
                             "Log in and try joining"))
                (let [id (db/uuid)
                      avatar (identicons/id->identicon-data-url id)
                      ; XXX: copied from chat.shared.util/nickname-rd
                      disallowed-chars #"[ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]"
                      nick (-> (first (string/split email #"@"))
                               (string/replace disallowed-chars ""))
                      u (db/with-conn (db/create-user! {:id id
                                                        :email email
                                                        :password (random-nonce 50)
                                                        :avatar avatar
                                                        :nickname nick}))
                      referer (get-in req [:headers "referer"] (env :site-url))
                      [proto _ referrer-domain] (string/split referer #"/")]
                  (db/with-conn
                    (db/user-add-to-group! id group-id)
                    (db/user-subscribe-to-group-tags! id group-id))
                  (sync/broadcast-user-change id [:chat/new-user u])
                  {:status 302 :headers {"Location" (str proto "//" referrer-domain)}
                   :session (assoc (req :session) :user-id id)
                   :body ""}))))))))

  (POST "/public-join" [group-id :as req]
    (let [bad-resp {:status 400 :headers {"Content-Type" "text/plain"}}]
      (if-not group-id
        (assoc bad-resp :body "Missing group id")
        (let [group-id (java.util.UUID/fromString group-id)
              group-settings (db/with-conn (db/group-settings group-id))]
          (if-not (:public? group-settings)
            (assoc bad-resp :body "No such group or the group is private")
            (if-let [user-id (get-in req [:session :user-id])]
              (do
                (when-not (db/with-conn (db/user-in-group? user-id group-id))
                  (db/with-conn
                    (db/user-add-to-group! user-id group-id)
                    (db/user-subscribe-to-group-tags! user-id group-id)
                    (let [other-users (->> (db/get-users-in-group group-id)
                                           (remove #(= user-id (:id %))))
                          user (db/user-by-id user-id)]
                      (doseq [uid other-users]
                        (sync/broadcast-user-change uid [:chat/new-user user])))))
                (let [referer (get-in req [:headers "referer"] (env :site-url))
                      [proto _ referrer-domain] (string/split referer #"/")]
                  {:status 302
                   :headers {"Location" (str proto "//" referrer-domain "/" group-id "/inbox")}
                   :body ""}))
              (assoc bad-resp :body "Not logged in")))))))

  ; reset password
  (POST "/reset" [new-password token user-id :<< as-uuid now hmac :as req]
    (let [fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? new-password) (assoc fail :body "Must provide a password")

        (not (invites/verify-hmac hmac (str now token user-id)))
        (assoc fail :body "Invalid HMAC")

        :else
        (if-let [user (db/with-conn (db/user-by-id user-id))]
          (if-let [err (:error (invites/verify-reset-nonce user token))]
            (assoc fail :body err)
            (let [referer (get-in req [:headers "referer"] (env :site-url))
                  [proto _ referrer-domain] (string/split referer #"/")]
              (db/with-conn (db/set-user-password! (user :id) new-password))
              {:status 301
               :headers {"Location" (str proto "//" referrer-domain)}
               :session (assoc (req :session) :user-id (user :id))
               :body ""}))
          (assoc fail :body "Invalid user"))))))

(defroutes extension-routes
  (context "/extension" _
    (GET "/oauth" [state code]
      (let [{ext-id :extension-id} (-> state b64->str edn/read-string)
            ext (db/with-conn (db/extension-by-id ext-id))]
        (if ext
          (do (ext/handle-oauth-token ext state code)
              {:status 302
               :headers {"Location" (str "/" (ext :group-id))}
               :body ""})
          {:status 400 :body "No such extension"})))
    (POST "/webhook/:ext" [ext :as req]
      (if-let [ext (db/with-conn (db/extension-by-id (java.util.UUID/fromString ext)))]
        (ext/handle-webhook ext req)
        {:status 400 :body "No such extension"}))
    (POST "/config" [extension-id data]
      (if-let [ext (db/with-conn (db/extension-by-id (java.util.UUID/fromString extension-id)))]
        (ext/extension-config ext data)
        {:status 400 :body "No such extension"}))))

(defroutes api-private-routes
  (GET "/extract" [url :as {ses :session}]
    (if (some? (db/with-conn (db/user-by-id (:user-id ses))))
      (edn-response (embedly/extract url))
      {:status 403
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:error "Unauthorized"})}))

  (GET "/s3-policy" req
    (if (some? (db/with-conn (db/user-by-id (get-in req [:session :user-id]))))
      (if-let [policy (s3/generate-policy)]
        {:status 200
         :headers {"Content-Type" "application/edn"}
         :body (pr-str policy)}
        {:status 500
         :headers {"Content-Type" "application/edn"}
         :body (pr-str {:error "No S3 secret for upload"})})
      {:status 403
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:error "Unauthorized"})})))

(defroutes resource-routes
  (resources "/"))
