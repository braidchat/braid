(ns braid.server.routes.api.public
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [compojure.coercions :refer [as-uuid]]
    [compojure.core :refer [GET PUT POST DELETE defroutes]]
    [braid.common.util :refer [valid-nickname? valid-email?]]
    [braid.server.api.embedly :as embedly]
    [braid.server.api.github :as github]
    [braid.server.crypto :refer [random-nonce]]
    [braid.server.conf :refer [config]]
    [braid.server.db :as db]
    [braid.server.db.group :as group]
    [braid.server.db.invitation :as invitation]
    [braid.server.db.user :as user]
    [braid.server.events :as events]
    [braid.server.invite :as invites]
    [braid.server.markdown :refer [markdown->hiccup]]
    [braid.server.s3 :as s3]
    [braid.server.sync :as sync]))

(defn user-from-session [req]
  (when-let [user-id (get-in req [:session :user-id])]
    (when-let [user (user/user-id-exists? user-id)]
      user)))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})

(defn join-group
  [user-id group-id]
  (when-not (group/user-in-group? user-id group-id)
    (events/user-join-group! user-id group-id)))

(defn error-response [status msg]
  {:status status
   :headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str {:error msg})})

(defroutes api-public-routes

  ; get current logged in user
  (GET "/session" req
    (if-let [user (user-from-session req)]
      (edn-response user)
      {:status 401 :body "" :session nil}))

  ; log in
  (PUT "/session" [email password :as req]
    (if-let [user-id (when (and email password)
                       (user/authenticate-user email password))]
      {:status 200
       :session (assoc (req :session) :user-id user-id)}
      (error-response 401 :auth-fail)))

  ; log out
  (DELETE "/session" _
    {:status 200 :session nil})

  ; register a user
  (PUT "/users" [email password :as req]
    (cond
      (string/blank? email)
      (error-response 400 "You must provide an email.")

      (not (valid-email? email))
      (error-response 400 "The email format is incorrect.")

      (user/user-with-email email)
      (error-response 400 :email-exists)

      (string/blank? password)
      (error-response 400 "You must provide a password.")

      (< (count password) 8)
      (error-response 400 "The password is too short.")

      :else
      (let [user (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                      :email email}))
            _ (db/run-txns! (user/set-user-password-txn (user :id)
                                                        password))]
        {:status 200
         :session (assoc (req :session) :user-id (user :id))})))

  (POST "/request-reset" [email]
    (if-let [user (user/user-with-email email)]
      (do (invites/request-reset (assoc user :email email))
          (edn-response {:ok true}))
      (error-response 400 :no-such-email)))

  (GET "/registration/check-slug-unique" req
    (edn-response (not (group/group-with-slug-exists? (get-in req [:params :slug])))))

  ; create a group
  ; requires authentication
  (PUT "/groups" [name slug type :as req]
    (if-let [user (user-from-session req)]
      (cond
        ; group name validations

        (string/blank? name)
        (error-response 400 "Must provide a Group Name")

        ; group url (slug) validations

        (string/blank? slug)
        (error-response 400 "Must provide an Group URL")

        (not (re-matches #"[a-z0-9-]*" slug))
        (error-response 400 "Group URL can only contain lowercase letters, numbers or dashes.")

        (re-matches #"-.*" slug)
        (error-response 400 "Group URL cannot start with a dash.")

        (re-matches #".*-" slug)
        (error-response 400 "Group URL cannot end with a dash.")

        (group/group-with-slug-exists? slug)
        (error-response 400 "A Group with this URL already exists.")

        ; group type validations

        (string/blank? type)
        (error-response 400 "Must provide a Group Type")

        (not (contains? #{"public" "private"} type))
        (error-response 400 "Group type must be either public or private")

        ; passed all validations
        :else
        (let [group-id (db/uuid)
              group (db/run-txns!
                      (group/create-group-txn {:id group-id
                                            :slug slug
                                            :name name}))]
          (db/run-txns! (group/group-set-txn (group :id) :public? (case type
                                                                    "public" true
                                                                    "private" false)))
          (db/run-txns! (group/user-add-to-group-txn (user :id) group-id))
          (db/run-txns! (group/user-subscribe-to-group-tags-txn (user :id) group-id))
          (db/run-txns! (group/user-make-group-admin-txn (user :id) group-id))
          (edn-response {:group-id group-id})))
      (error-response 401 "")))

  ; accept an email invite to join a group
  (POST "/register" [token invite_id nickname password email now hmac avatar :as req]
    (let [fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? password) (assoc fail :body "Must provide a password")

        (not (invites/verify-hmac hmac (str now token invite_id email)))
        (assoc fail :body "Invalid HMAC")

        (string/blank? invite_id)
        (assoc fail :body "Invalid invitation ID")

        (not (valid-nickname? nickname))
        (assoc fail :body "Nickname must be 1-30 characters without whitespace")

        (user/nickname-taken? nickname)
        (assoc fail :body "nickname taken")

        ; TODO: be smarter about this
        (not (#{"image/jpeg" "image/png"} (:content-type avatar)))
        (assoc fail :body "Invalid image")

        :else
        (let [invite (invitation/invite-by-id (java.util.UUID/fromString invite_id))]
          (if-let [err (:error (invites/verify-invite-nonce invite token))]
            (assoc fail :body "Invalid invite token")
            (let [avatar-url (invites/upload-avatar avatar)
                  user-id (db/uuid)
                  referer (get-in req [:headers "referer"] (config :site-url))
                  [proto _ referrer-domain] (string/split referer #"/")]
              (do
                (db/run-txns!
                  (user/create-user-txn {:id user-id
                                         :email email
                                         :avatar avatar-url
                                         :nickname nickname
                                         :password password}))
                (events/user-join-group! user-id (invite :group-id))
                (db/run-txns!
                  (concat
                    (invitation/retract-invitation-txn (invite :id)))))
              {:status 302
               :headers {"Location" (str proto "//" referrer-domain)}
               :session (assoc (req :session) :user-id user-id)
               :body ""}))))))

  ; join by invite link
  (POST "/link-register" [group-id email now form-hmac :as req]
    (let [bad-resp {:status 400 :headers {"Content-Type" "text/plain"}}]
      (if-not group-id
        (assoc bad-resp :body "Missing group id")
        (let [group-id (java.util.UUID/fromString group-id)
              group-settings (group/group-settings group-id)]
          (if-not (invites/verify-hmac form-hmac (str now group-id))
            (assoc bad-resp :body "No such group or the request has been tampered with")
            (if (string/blank? email)
              (assoc bad-resp :body "Invalid email")
              (if (user/user-with-email email)
                (assoc bad-resp
                       :body (str "A user is already registered with that email.\n"
                                  "Log in and try joining"))
                (let [id (events/register-user! email group-id)
                      referer (get-in req [:headers "referer"] (config :site-url))
                      [proto _ referrer-domain] (string/split referer #"/")]
                  {:status 302 :headers {"Location" (str proto "//" referrer-domain)}
                   :session (assoc (req :session) :user-id id)
                   :body ""}))))))))

  (POST "/link-join" [group-id now form-hmac :as req]
    (let [bad-resp {:status 400 :headers {"Content-Type" "text/plain"}}]
      (if-not group-id
        (assoc bad-resp :body "Missing group id")
        (let [group-id (java.util.UUID/fromString group-id)
              group-settings (group/group-settings group-id)]
          (if-not (invites/verify-hmac form-hmac (str now group-id))
            (assoc bad-resp :body "No such group or the request has been tampered with")
            (if-let [user-id (get-in req [:session :user-id])]
              (do
                (join-group user-id group-id)
                (let [referer (get-in req [:headers "referer"] (config :site-url))
                      [proto _ referrer-domain] (string/split referer #"/")]
                  {:status 302
                   :headers {"Location" (str proto "//" referrer-domain "/" group-id "/inbox")}
                   :body ""}))
              (assoc bad-resp :body "Not logged in")))))))

  ; join a public group
  (POST "/public-register" [group-id email :as req]
    (let [bad-resp {:status 400 :headers {"Content-Type" "text/plain"}}]
      (if-not group-id
        (assoc bad-resp :body "Missing group id")
        (let [group-id (java.util.UUID/fromString group-id)
              group-settings (group/group-settings group-id)]
          (if-not (get group-settings :public?)
            (assoc bad-resp :body "No such group or the group is private")
            (if (string/blank? email)
              (assoc bad-resp :body "Invalid email")
              (if (user/user-with-email email)
                (assoc bad-resp
                       :body (str "A user is already registered with that email.\n"
                                  "Log in and try joining"))
                (let [id (events/register-user! email group-id)
                      referer (get-in req [:headers "referer"] (config :site-url))
                      [proto _ referrer-domain] (string/split referer #"/")]
                  {:status 302 :headers {"Location" (str proto "//" referrer-domain)}
                   :session (assoc (req :session) :user-id id)
                   :body ""}))))))))

  (POST "/public-join" [group-id :as req]
    (let [bad-resp {:status 400 :headers {"Content-Type" "text/plain"}}]
      (if-not group-id
        (assoc bad-resp :body "Missing group id")
        (let [group-id (java.util.UUID/fromString group-id)
              group-settings (group/group-settings group-id)]
          (if-not (:public? group-settings)
            (assoc bad-resp :body "No such group or the group is private")
            (if-let [user-id (get-in req [:session :user-id])]
              (do
                (join-group user-id group-id)
                (let [referer (get-in req [:headers "referer"] (config :site-url))
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
        (if-let [user (user/user-by-id user-id)]
          (if-let [err (:error (invites/verify-reset-nonce user token))]
            (assoc fail :body err)
            (let [referer (get-in req [:headers "referer"] (config :site-url))
                  [proto _ referrer-domain] (string/split referer #"/")]
              (db/run-txns! (user/set-user-password-txn (user :id) new-password))
              {:status 302
               :headers {"Location" (str proto "//" referrer-domain)}
               :session (assoc (req :session) :user-id (user :id))
               :body ""}))
          (assoc fail :body "Invalid user")))))

    ; OAuth dance
    (GET "/oauth/github"  [code state :as req]
      (println "GITHUB OAUTH" (pr-str code) (pr-str state))
      (if-let [{tok :access_token scope :scope :as resp}
               (github/exchange-token code state)]
        (do (println "GITHUB TOKEN" tok)
            ; check scope includes email permission? Or we could just see if
            ; getting the email fails
            (let [email (github/email-address tok)
                  user (user/user-with-email email)]
              (cond
                (nil? email) {:status 401
                              :headers {"Content-Type" "text/plain"}
                              :body "Couldn't get email address from github"}

                user {:status 302
                      ; TODO: when we have mobile, redirect to correct site
                      ; (maybe part of state?)
                      :headers {"Location" (config :site-url)}
                      :session (assoc (req :session) :user-id (user :id))}

                (:braid.server.api/register? resp)
                (let [user-id (events/register-user! email (:braid.server.api/group-id resp))]
                  {:status 302
                   ; TODO: when we have mobile, redirect to correct site
                   ; (maybe part of state?)
                   :headers {"Location" (config :site-url)}
                   :session (assoc (req :session) :user-id user-id)})

                :else
                {:status 401
                 ; TODO: handle failure better
                 :headers {"Content-Type" "text/plain"}
                 :body "No such user"
                 :session nil})))
        {:status 400
         :body "Couldn't exchange token with github"})))

(defroutes api-private-routes
  (GET "/changelog" []
    (edn-response {:braid/ok
                   (-> (io/resource "CHANGELOG.md")
                       slurp markdown->hiccup)}))

  (GET "/extract" [url :as {ses :session}]
    (if (some? (user/user-by-id (:user-id ses)))
      (edn-response (embedly/extract url))
      {:status 403
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:error "Unauthorized"})}))

  (GET "/s3-policy" req
    (if (some? (user/user-by-id (get-in req [:session :user-id])))
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