(ns braid.core.server.routes.api.public
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [compojure.coercions :refer [as-uuid]]
    [compojure.core :refer [GET PUT POST defroutes]]
    [braid.core.common.util :refer [valid-nickname? valid-email?]]
    [braid.core.hooks :as hooks]
    [braid.core.server.api.github :as github]
    [braid.core.server.conf :refer [config]]
    [braid.core.server.crypto :refer [random-nonce]]
    [braid.core.server.db :as db]
    [braid.core.server.db.group :as group]
    [braid.core.server.db.invitation :as invitation]
    [braid.core.server.db.user :as user]
    [braid.core.server.events :as events]
    [braid.core.server.invite :as invites]
    [braid.core.server.markdown :refer [markdown->hiccup]]
    [braid.core.server.routes.helpers :refer [current-user error-response edn-response]]
    [braid.core.server.s3 :as s3]
    [braid.core.server.sync :as sync]))

(defn join-group
  [user-id group-id]
  (when-not (group/user-in-group? user-id group-id)
    (events/user-join-group! user-id group-id)))

(defroutes api-public-routes

  (GET "/groups" _
    (edn-response (group/public-groups)))

  (GET "/groups/:group-id" [group-id]
       (if-let [group (try
                        (group/group-by-id (java.util.UUID/fromString group-id))
                        (catch java.lang.IllegalArgumentException _
                          nil))]
         (if (:public? group) ; XXX: should it also give info for private groups?
           (edn-response {:public? (group :public?)
                          :id (group :id)
                          :slug (group :slug)
                          :name (group :name)
                          :intro (group :intro)
                          :avatar (group :avatar)
                          :users-count (group :users-count)})
           ;; XXX: should it just give 404? is it okay to let people guess private ids?
           (error-response 401 "Group is private"))
         (error-response 404 "No group with that id found")))

  ; log in
  (PUT "/session" [email password :as req]
    (if-let [user-id (when (and email password)
                       (user/authenticate-user email password))]
      {:status 200
       :session (assoc (req :session) :user-id user-id)}
      (error-response 401 :auth-fail)))

  (PUT "/session/oauth/github" [code state :as req]
    (if-let [{:keys [access_token]} (github/exchange-token code state)]
      (let [email (github/email-address access_token)
            user (user/user-with-email email)]
        (cond
          (nil? email)
          (error-response 500 "Couldn't get email from Github")

          user
          {:status 200
           :session (assoc (req :session) :user-id (user :id))}

          :else
          (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
                                                            :email email}))]
            {:status 200
             :session (assoc (req :session) :user-id (user :id))})))
      (error-response 500 "Couldn't exchange token with github")))

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
      (let [[user] (db/run-txns! (user/create-user-txn {:id (db/uuid)
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
                   :headers {"Location" (str proto "//" referrer-domain "/groups/" group-id)}
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
          (assoc fail :body "Invalid user"))))))
