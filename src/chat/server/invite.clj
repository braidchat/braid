(ns chat.server.invite
  (:require [org.httpkit.client :as http]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [taoensso.carmine :as car]
            [clojure.string :as string]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [aws.sdk.s3 :as s3])
  (:import java.security.SecureRandom
           javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec
           [org.apache.commons.codec.binary Base64]))

(defn random-nonce
  "url-safe random nonce"
  [size]
  (let [rand-bytes (let [seed (byte-array size)]
                     (.nextBytes (SecureRandom. ) seed)
                     seed)]
    (-> rand-bytes
        Base64/encodeBase64
        String.
        (string/replace "+" "-")
        (string/replace "/" "_")
        (string/replace "=" ""))))

(defn hmac
  [hmac-key data]
  (let [key-bytes (.getBytes hmac-key "UTF-8")
        data-bytes (.getBytes data "UTF-8")
        algo "HmacSHA256"]
    (->>
      (doto (Mac/getInstance algo)
        (.init (SecretKeySpec. key-bytes algo)))
      (#(.doFinal % data-bytes))
      (map (partial format "%02x"))
      (apply str))))

(defn constant-comp
  "Compare two strings in constant time"
  [a b]
  (loop [a a b b match (= (count a) (count b))]
    (if (and (empty? a) (empty? b))
      match
      (recur
        (rest a)
        (rest b)
        (and match (= (first a) (first b)))))))

(defn verify-hmac
  [mac data]
  (constant-comp
    mac
    (hmac (env :hmac-secret) data)))

; same as conf in handler, but w/e
(def redis-conn {:pool {}
                 :spec {:host "127.0.0.1"
                        :port 6379}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn make-invite-link
  [invite]
  (let [secret-nonce (random-nonce 20)]
    (wcar* (car/set (str (invite :id)) secret-nonce))
    (str (env :site-url)
         "/accept?tok=" secret-nonce
         "&invite=" (invite :id))))

(defn verify-invite-nonce
  "Verify that the given nonce is valid for the invite"
  [invite nonce]
  (if-let [stored-nonce (wcar* (car/get (str (invite :id))))]
    (if (constant-comp stored-nonce nonce)
      (do (wcar* (car/del (str (invite :id))))
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
  (http/post (str "https://api.mailgun.net/v3/" (env :mailgun-domain) "/messages")
             {:basic-auth ["api" (env :mailgun-password)]
              :form-params (merge {:to (invite :invitee-email)
                                   :from (str "noreply@" (env :mailgun-domain))
                                   :subject "Join the conversation"}
                                  (invite-message invite))}))

(defn register-page
  [invite token]
  (let [now (.getTime (java.util.Date.))
        form-hmac (hmac (env :hmac-secret)
                        (str now token (invite :id) (invite :invitee-email)))]
    (str "<!DOCTYPE html>"
         "<html>"
         "  <head>"
         "   <title>Register for Chat</title>"
         "   <link href=\"/css/out/chat.css\" rel=\"stylesheet\" type=\"text/css\"></style>"
         "  </head>"
         "  <body>"
         "  <p>Upload an avatar for " (invite :invitee-email) "</p>"
         "  <form action=\"/register\" method=\"POST\" enctype=\"multipart/form-data\">"
         "    <input type=\"hidden\" name=\"csrf-token\" value=\"" *anti-forgery-token* "\">"
         "    <input type=\"hidden\" name=\"token\" value=\"" token "\">"
         "    <input type=\"hidden\" name=\"invite_id\" value=\"" (invite :id) "\">"
         "    <input type=\"hidden\" name=\"email\" value=\"" (invite :invitee-email) "\">"
         "    <input type=\"hidden\" name=\"now\" value=\"" now "\">"
         "    <input type=\"hidden\" name=\"hmac\" value=\"" form-hmac "\">"
         "    <input type=\"file\" name=\"avatar\" size=\"20\">"
         "    <label>Password: <input type=\"password\" name=\"password\"></label>"
         "    <input type=\"submit\" value=\"Register\")>"
         "  </form>"
         "  </body>"
         "</html>")))

(defn verify-form-hmac
  [params]
  (constant-comp
    (params :hmac)
    (hmac (env :hmac-secret)
          (str (params :now) (params :token) (params :invite_id) (params :email)))))

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
        avatar-filename (str (java.util.UUID/randomUUID) "." ext)]
    (s3/put-object creds (env :aws-domain) (str "avatars/" avatar-filename) (f :tempfile)
                   {:content-type (f :content-type)})
    (s3/update-object-acl creds (env :aws-domain) (str "avatars/" avatar-filename) (s3/grant :all-users :read))
    (str "https://s3.amazonaws.com/" (env :aws-domain) "/avatars/" avatar-filename)))
