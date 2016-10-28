(ns braid.server.routes.api.public
  (:require [clojure.string :as string]
            [compojure.core :refer [GET POST PUT defroutes]]
            [compojure.coercions :refer [as-uuid]]
            [braid.common.util :refer [valid-nickname? valid-email?]]
            [braid.server.db :as db]
            [braid.server.invite :as invites]
            [braid.server.api.github :as github]
            [braid.server.conf :refer [config]]
            [braid.server.events :as events]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})

(defn join-group
  [user-id group-id]
  (when-not (db/user-in-group? user-id group-id)
    (events/user-join-group! user-id group-id)))

(defroutes api-public-routes
  ; check if already logged in
  (GET "/check" req
    (if-let [user-id (get-in req [:session :user-id])]
      (if-let [user (db/user-id-exists? user-id)]
        {:status 200 :body ""}
        {:status 401 :body "" :session nil})
      {:status 401 :body "" :session nil}))

  ; log in
  (POST "/auth" req
    (if-let [user-id (let [{:keys [email password]} (req :params)]
                       (when (and email password)
                         (db/authenticate-user email password)))]
      {:status 200 :session (assoc (req :session) :user-id user-id)}
      {:status 401 :body (pr-str {:error true})}))
  ; log out
  (POST "/logout" req
    {:status 200 :session nil})

  ; request a password reset
  (POST "/request-reset" [email]
    (when-let [user (db/user-with-email email)]
      (invites/request-reset (assoc user :email email)))
    {:status 200 :body (pr-str {:ok true})})

  (GET "/registration/check-slug-unique" req
    (edn-response (not (db/group-with-slug-exists? (get-in req [:params :slug])))))

  (PUT "/registration/register" [email name slug type :as req]
    (let [fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        ; email validations

        (string/blank? email)
        (assoc fail :body "Must provide an Email")

        (not (valid-email? email))
        (assoc fail :body "Email format is incorrect")

        (db/user-with-email email)
        (assoc fail :body "A user is already registered with that email.")

        ; group name validations

        (string/blank? name)
        (assoc fail :body "Must provide a Group Name")

        ; group url (slug) validations

        (string/blank? slug)
        (assoc fail :body "Must provide an Group URL")

        (not (re-matches #"[a-z0-9-]*" slug))
        (assoc fail :body "Group URL can only contain lowercase letters, numbers or dashes.")

        (re-matches #"-.*" slug)
        (assoc fail :body "Group URL cannot start with a dash.")

        (re-matches #".*-" slug)
        (assoc fail :body "Group URL cannot end with a dash.")

        (db/group-with-slug-exists? slug)
        (assoc fail :body "A Group with this URL already exists.")

        ; group type validations

        (string/blank? type)
        (assoc fail :body "Must provide a Group Type")

        (not (contains? #{"public" "private"} type))
        (assoc fail :body "Group type must be either public or private")

        ; passed all validations
        :else
        (let [group (db/create-group! {:id (db/uuid)
                                       :slug slug
                                       :name name})
              user (db/create-user! {:id (db/uuid)
                                     :email email})]

          (db/user-add-to-group! (user :id) (group :id))
          (db/user-make-group-admin! (user :id) (group :id))

          (edn-response true)))))

  ; accept an email invite to join a group
  (POST "/register" [token invite_id password email now hmac nickname avatar :as req]
    (let [fail {:status 400 :headers {"Content-Type" "text/plain"}}]
      (cond
        (string/blank? password) (assoc fail :body "Must provide a password")

        (not (invites/verify-hmac hmac (str now token invite_id email)))
        (assoc fail :body "Invalid HMAC")

        (string/blank? invite_id)
        (assoc fail :body "Invalid invitation ID")

        (not (valid-nickname? nickname))
        (assoc fail :body "Nickname must be 1-30 characters without whitespace")

        (db/nickname-taken? nickname)
        (assoc fail :body "nickname taken")

        ; TODO: be smarter about this
        (not (#{"image/jpeg" "image/png"} (:content-type avatar)))
        (assoc fail :body "Invalid image")

        :else
        (let [invite (db/invite-by-id (java.util.UUID/fromString invite_id))]
          (if-let [err (:error (invites/verify-invite-nonce invite token))]
            (assoc fail :body "Invalid invite token")
            (let [avatar-url (invites/upload-avatar avatar)
                  user (db/create-user! {:id (db/uuid)
                                         :email email
                                         :avatar avatar-url
                                         :nickname nickname
                                         :password password})
                  referer (get-in req [:headers "referer"] (config :site-url))
                  [proto _ referrer-domain] (string/split referer #"/")]
              (do
                (events/user-join-group! (user :id) (invite :group-id))
                (db/retract-invitation! (invite :id)))
              {:status 302
               :headers {"Location" (str proto "//" referrer-domain)}
               :session (assoc (req :session) :user-id (user :id))
               :body ""}))))))

  ; join by invite link
  (POST "/link-register" [group-id email now form-hmac :as req]
    (let [bad-resp {:status 400 :headers {"Content-Type" "text/plain"}}]
      (if-not group-id
        (assoc bad-resp :body "Missing group id")
        (let [group-id (java.util.UUID/fromString group-id)
              group-settings (db/group-settings group-id)]
          (if-not (invites/verify-hmac form-hmac (str now group-id))
            (assoc bad-resp :body "No such group or the request has been tampered with")
            (if (string/blank? email)
              (assoc bad-resp :body "Invalid email")
              (if (db/user-with-email email)
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
              group-settings (db/group-settings group-id)]
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
              group-settings (db/group-settings group-id)]
          (if-not (get group-settings :public?)
            (assoc bad-resp :body "No such group or the group is private")
            (if (string/blank? email)
              (assoc bad-resp :body "Invalid email")
              (if (db/user-with-email email)
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
              group-settings (db/group-settings group-id)]
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
        (if-let [user (db/user-by-id user-id)]
          (if-let [err (:error (invites/verify-reset-nonce user token))]
            (assoc fail :body err)
            (let [referer (get-in req [:headers "referer"] (config :site-url))
                  [proto _ referrer-domain] (string/split referer #"/")]
              (db/set-user-password! (user :id) new-password)
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
                user (db/user-with-email email)]
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
