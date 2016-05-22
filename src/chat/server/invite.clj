(ns chat.server.invite
  (:require [org.httpkit.client :as http]
            [clojure.string :as string]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [aws.sdk.s3 :as s3]
            [taoensso.carmine :as car]
            [image-resizer.core :as img]
            [image-resizer.format :as img-format]
            [chat.server.cache :refer [cache-set! cache-get cache-del! random-nonce]]
            [chat.server.crypto :refer [hmac constant-comp]]
            [chat.server.conf :refer [api-port]]))

(when (and (= (env :environment) "prod") (empty? (env :hmac-secret)))
  (println "WARNING: No :hmac-secret set, using an insecure default."))

(def hmac-secret (or (env :hmac-secret) "secret"))

(defn verify-hmac
  [mac data]
  (constant-comp
    mac
    (hmac hmac-secret data)))

(defn make-invite-link
  [invite]
  (let [secret-nonce (random-nonce 20)]
    (cache-set! (str (invite :id)) secret-nonce)
    (str (env :site-url)
         "/accept?tok=" secret-nonce
         "&invite=" (invite :id))))

(defn verify-invite-nonce
  "Verify that the given nonce is valid for the invite"
  [invite nonce]
  (if-let [stored-nonce (cache-get (str (invite :id)))]
    (if (constant-comp stored-nonce nonce)
      (do (cache-del! (str (invite :id)))
          {:success true})
      {:error "Invalid token"})
    (do (timbre/warnf "Expired nonce %s for invite %s" nonce invite)
        {:error "Expired token"})))

(defn invite-message
  [invite]
  (let [accept-link (make-invite-link invite)]
    {:text (str (invite :inviter-email) " has invited to join the " (invite :group-name)
                " group on " (env :site-url) ".\n\n"
                "Go to " accept-link " to accept.")
     :html (str "<html><body>"
                (invite :inviter-email) " has invited to join the " (invite :group-name)
                " group on <a href=\"" (env :site-url) "\">" (env :site-url) "</a>."
                "<br>"
                "<a href=\"" accept-link "\">Click here</a> to accept."
                "</body></html>")}))

(defn send-invite
  [invite]
  @(http/post (str "https://api.mailgun.net/v3/" (env :mailgun-domain) "/messages")
             {:basic-auth ["api" (env :mailgun-password)]
              :form-params (merge {:to (invite :invitee-email)
                                   :from (str "noreply@" (env :mailgun-domain))
                                   :subject "Join the conversation"}
                                  (invite-message invite))}))

(defn register-page
  [invite token]
  (let [now (.getTime (java.util.Date.))
        form-hmac (hmac hmac-secret
                        (str now token (invite :id) (invite :invitee-email)))
        api-domain (or (:api-domain env) (str "localhost:" @api-port))]
    ; TODO: use mustache instead
    (str "<!DOCTYPE html>"
         "<html>"
         "  <head>"
         "   <title>Register for Chat</title>"
         "   <link href=\"/css/out/chat.css\" rel=\"stylesheet\" type=\"text/css\"></style>"
         "  </head>"
         "  <body>"
         "  <p>Upload an avatar for " (invite :invitee-email) "</p>"
         "  <form action=\"//" api-domain "/register\" method=\"POST\" enctype=\"multipart/form-data\">"
         "    <input type=\"hidden\" name=\"token\" value=\"" token "\">"
         "    <input type=\"hidden\" name=\"invite_id\" value=\"" (invite :id) "\">"
         "    <input type=\"hidden\" name=\"email\" value=\"" (invite :invitee-email) "\">"
         "    <input type=\"hidden\" name=\"now\" value=\"" now "\">"
         "    <input type=\"hidden\" name=\"hmac\" value=\"" form-hmac "\">"
         "    <input type=\"file\" name=\"avatar\" size=\"20\">"
         "    <label>Nickname: <input type=\"text\" name=\"nickname\"></label>"
         "    <label>Password: <input type=\"password\" name=\"password\"></label>"
         "    <input type=\"submit\" value=\"Register\")>"
         "  </form>"
         "  </body>"
         "</html>")))

(defn verify-form-hmac
  [params]
  (constant-comp
    (params :hmac)
    (hmac hmac-secret
          (str (params :now) (params :token) (params :invite_id) (params :email)))))

(def avatar-size [128 128])

(defn upload-avatar
  [f]
  ; TODO: resize avatar to something reasonable
  (let [creds {:access-key (env :aws-access-key)
               :secret-key (env :aws-secret-key)}
        ext (case (f :content-type)
              "image/jpeg" "jpg"
              "image/png" "png"
              ; TODO
              (last (string/split (f :filename) #"\.")))
        avatar-filename (str (java.util.UUID/randomUUID) "." ext)
        [h w] avatar-size
        resized-image (-> f :tempfile (img/resize-and-crop h w)
                          (img-format/as-stream ext))]
    (s3/put-object creds (env :aws-domain) (str "avatars/" avatar-filename) resized-image
                   {:content-type (f :content-type)})
    (s3/update-object-acl creds (env :aws-domain) (str "avatars/" avatar-filename)
                          (s3/grant :all-users :read))
    (str "https://s3.amazonaws.com/" (env :aws-domain) "/avatars/" avatar-filename)))

(defn request-reset
  [user]
  (let [secret (random-nonce 20)]
    (cache-set! (str (user :id)) secret)
    @(http/post (str "https://api.mailgun.net/v3/" (env :mailgun-domain) "/messages")
                {:basic-auth ["api" (env :mailgun-password)]
                 :form-params {:to (user :email)
                               :from (str "noreply@" (env :mailgun-domain))
                               :subject "Password reset requested"
                               :text (str "A password reset was requested on "
                                          (env :site-url) " for " (user :email) "\n\n"
                                          "If this was you, follow this link to reset your password "
                                          (env :site-url) "/reset?user=" (user :id) "&token=" secret
                                          "\n\n"
                                          "If this wasn't you, just ignore this email")
                               :html (str "<html><body>"
                                          "<p>"
                                          "A password reset was requested on "
                                          (env :site-url) " for " (user :email)
                                          "</p>"
                                          "<p>"
                                          "If this was you, <a href=\""
                                          (env :site-url) "/reset?user=" (user :id) "&token=" secret
                                          "\"> click here</a> to reset your password "
                                          "</p>"
                                          "<p>"
                                          "If this wasn't you, just ignore this email"
                                          "</p>"
                                          "</body></html>")}})))

(defn verify-reset-nonce
  "Verify that the given nonce is valid for the reset request"
  [user nonce]
  (if-let [stored-nonce (cache-get (str (user :id)))]
    (if (constant-comp stored-nonce nonce)
      (do {:success true})
      {:error "Invalid token"})
    (do (timbre/warnf "Expired nonce %s for user %s" nonce user)
        {:error "Expired token"})))

(defn invalidate-reset-nonce!
 [user]
 (cache-del! (str (user :id))))

(defn reset-page
  [user token]
  (let [now (.getTime (java.util.Date.))
        form-hmac (hmac hmac-secret (str now token (user :id)))
        api-domain (or (:api-domain env) (str "localhost:" @api-port))]
    ; TODO: use mustache instead
    (str "<!DOCTYPE html>"
         "<html>"
         "  <head>"
         "   <title>Reset Password</title>"
         "   <link href=\"/css/out/chat.css\" rel=\"stylesheet\" type=\"text/css\"></style>"
         "  </head>"
         "  <body>"
         "  <form action=\"//" api-domain "/reset\" method=\"POST\">"
         "    <input type=\"hidden\" name=\"user-id\" value=\"" (user :id) "\">"
         "    <input type=\"hidden\" name=\"now\" value=\"" now "\">"
         "    <input type=\"hidden\" name=\"token\" value=\"" token "\">"
         "    <input type=\"hidden\" name=\"hmac\" value=\"" form-hmac "\">"
         "    <label>New Password: <input type=\"password\" name=\"new-password\"></label>"
         "    <input type=\"submit\" value=\"Set New Password\">"
         "  </form>"
         "  </body>"
         "</html>")))
