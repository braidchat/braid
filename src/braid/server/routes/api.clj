(ns braid.server.routes.api
  (:require [clojure.string :as string]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.coercions :refer [as-uuid]]
            [clojure.java.io :as io]
            [braid.common.util :refer [valid-nickname?]]
            [braid.server.db :as db]
            [braid.server.invite :as invites]
            [braid.server.identicons :as identicons]
            [braid.server.crypto :refer [random-nonce]]
            [braid.server.sync :as sync]
            [braid.server.s3 :as s3]
            [braid.server.api.embedly :as embedly]
            [braid.server.api.github :as github]
            [braid.server.conf :refer [config]]
            [braid.server.markdown :refer [markdown->hiccup]]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8"}
   :body (pr-str clj-body)})

(defn register-user
  [email group-id]
  (let [id (db/uuid)
        avatar (identicons/id->identicon-data-url id)
        ; XXX: copied from braid.common.util/nickname-rd
        disallowed-chars #"[ \t\n\]\[!\"#$%&'()*+,.:;<=>?@\^`{|}~/]"
        nick (-> (first (string/split email #"@"))
                 (string/replace disallowed-chars ""))
        ; TODO: guard against duplicate nickname?
        u (db/create-user! {:id id
                            :email email
                            :password (random-nonce 50)
                            :avatar avatar
                            :nickname nick})]
    (sync/user-join-group! id group-id)
    id))

(defn join-group
  [user-id group-id]
  (when-not (db/user-in-group? user-id group-id)
    (sync/user-join-group! user-id group-id)))

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
                (sync/user-join-group! (user :id) (invite :group-id))
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
                (let [id (register-user email group-id)
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
                (let [id (register-user email group-id)
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
                (let [user-id (register-user email (:braid.server.api/group-id resp))]
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
    (if (some? (db/user-by-id (:user-id ses)))
      (edn-response (embedly/extract url))
      {:status 403
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:error "Unauthorized"})}))

  (GET "/s3-policy" req
    (if (some? (db/user-by-id (get-in req [:session :user-id])))
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
