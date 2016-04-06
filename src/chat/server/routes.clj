(ns chat.server.routes
  (:require
    [clojure.string :as string]
    [clojure.edn :as edn]
    [compojure.core :refer [GET POST defroutes context]]
    [compojure.route :refer [resources]]
    [chat.shared.util :refer [valid-nickname?]]
    [chat.server.digest :as digest]
    [chat.server.db :as db]
    [chat.server.invite :as invites]
    [chat.server.identicons :as identicons]
    [chat.server.cache :refer [random-nonce]]
    [chat.server.sync :as sync]
    [chat.server.extensions :as ext :refer [b64->str]]
    [chat.server.s3 :as s3]
    [environ.core :refer [env]]))

(def api-port (atom nil))

(defn- edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})

(defn- get-html [client]
  (let [replacements {"{{algo}}" "sha256"
                      "{{js}}" (str (digest/from-file (str "/js/" client "/out/braid.js")))
                      "{{api_domain}}" (or (:api-domain env) (str "localhost:" @api-port))}
        html (-> (str "public/" client ".html")
                 clojure.java.io/resource
                 slurp)]
    (string/replace html #"\{\{\w*\}\}" replacements)))

(defroutes desktop-client-routes
  (GET "/*" []
    (get-html "desktop")))

(defroutes mobile-client-routes
  (GET "/*" []
    (get-html "mobile")))

(defroutes api-public-routes
  (GET "/accept" [invite tok]
    (if (and invite tok)
      (if-let [invite (db/with-conn (db/get-invite (java.util.UUID/fromString invite)))]
        {:status 200 :headers {"Content-Type" "text/html"} :body (invites/register-page invite tok)}
        {:status 400 :headers {"Content-Type" "text/plain"} :body "Invalid invite"})
      {:status 400 :headers {"Content-Type" "text/plain"} :body "Bad invite link, sorry"}))

  (POST "/public-register/:group-id" [group-id email :as req]
    (let [group-id (java.util.UUID/fromString group-id)
          group-settings (db/with-conn (db/group-settings group-id))]
      (if-not (get group-settings :public?)
        {:status 400 :body "No such group or the group is private"}
        (if (string/blank? email)
          {:status 400 :body "Invalid email"}
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
                                                  :nickname nick}))]
            (db/with-conn
              (db/user-add-to-group! id group-id)
              (db/user-subscribe-to-group-tags! id group-id))
            (sync/broadcast-user-change id [:chat/new-user u])
            {:status 302 :headers {"Location" "/"}
             :session (assoc (req :session) :user-id id)
             :body ""})))))

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
                                                       :password password}))]
              (db/with-conn
                (db/user-add-to-group! (user :id) (invite :group-id))
                (db/user-subscribe-to-group-tags! (user :id) (invite :group-id))
                (db/retract-invitation! (invite :id)))
              (sync/broadcast-user-change (user :id) [:chat/new-user user])
              {:status 302 :headers {"Location" "/"}
               :session (assoc (req :session) :user-id (user :id))
               :body ""}))))))

  (POST "/logout" req
    {:status 200 :session nil}))

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
       :body (pr-str {:error "Unauthorized"})}))
  (POST "/auth" req
    (println "Params" req )
    (if-let [user-id (let [{:keys [email password]} (req :params)]
                       (when (and email password)
                         (db/with-conn (db/authenticate-user email password))))]
      (do (println "AUTH!" user-id) {:status 200 :session (assoc (req :session) :user-id user-id)})
      {:status 401 :body (pr-str {:error true})}))
  (POST "/request-reset" [email]
    (when-let [user (db/with-conn (db/user-with-email email))]
      (invites/request-reset (assoc user :email email)))
    {:status 200 :body (pr-str {:ok true})})
  (GET "/reset" [user token]
    (if-let [u (and (invites/verify-reset-nonce user token)
                 (db/with-conn (db/user-by-id (java.util.UUID/fromString user))))]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (invites/reset-page u token)}
      {:status 401}))
  (POST "/reset" [new_password token user_id now hmac :as req]
    (let [user-id (java.util.UUID/fromString user_id)
          fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? new_password) (assoc fail :body "Must provide a password")

        (not (invites/verify-hmac hmac (str now token user-id)))
        (assoc fail :body "Invalid HMAC")

        :else
        (if-let [user (db/with-conn (db/user-by-id user-id))]
          (if-let [err (:error (invites/verify-reset-nonce user token))]
            (assoc fail :body err)
            (do (db/with-conn (db/set-user-password! (user :id) new_password))
                {:status 301
                 :headers {"Location" "/"}
                 :session (assoc (req :session) :user-id (user :id))
                 :body ""}))
          (assoc fail :body "Invalid user"))))))

(defroutes resource-routes
  (resources "/"))
