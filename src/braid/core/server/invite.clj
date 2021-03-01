(ns braid.core.server.invite
  (:require
   [braid.base.server.cache :refer [cache-set! cache-get cache-del!]]
   [braid.base.conf :refer [config]]
   [braid.chat.db.group :as group]
   [braid.core.server.mail :as mail]
   [braid.lib.crypto :refer [hmac constant-comp random-nonce]]
   [clj-time.coerce :as c]
   [clj-time.core :as t]
   [clojure.string :as string]
   [cljstache.core :as cljstache]
   [image-resizer.core :as img]
   [image-resizer.format :as img-format]
   [org.httpkit.client :as http]
   [taoensso.timbre :as timbre]))

(defn verify-hmac
  [mac data]
  (constant-comp
    mac
    (hmac (config :hmac-secret) data)))

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

;; invite by email
(defn make-email-invite-link
  [invite]
  (let [secret-nonce (random-nonce 20)]
    (cache-set! (str (invite :id)) secret-nonce)
    (str (config :site-url)
         "/accept?tok=" secret-nonce
         "&invite=" (invite :id))))

(defn invite-message
  [invite]
  (let [accept-link (make-email-invite-link invite)]
    {:text (str (invite :inviter-email) " has invited to join the " (invite :group-name)
                " group on " (config :site-url) ".\n\n"
                "Go to " accept-link " to accept.")
     :html (str "<html><body>"
                (invite :inviter-email) " has invited to join the " (invite :group-name)
                " group on <a href=\"" (config :site-url) "\">" (config :site-url) "</a>."
                "<br>"
                "<a href=\"" accept-link "\">Click here</a> to accept."
                "</body></html>")}))

(defn send-invite
  [invite]
  (mail/send!
    {:to (invite :invitee-email)
     :subject "Join the conversation"
     :body (invite-message invite)}))

(defn register-page
  [invite token]
  (let [now (.getTime (java.util.Date.))
        form-hmac (hmac (config :hmac-secret)
                        (str now token (invite :id) (invite :invitee-email)))]
    (cljstache/render-resource "templates/register_page.html.mustache"
                               {:invitee_email (invite :invitee-email)
                                :inviter (invite :inviter-email)
                                :group (invite :group-name)
                                :token token
                                :invite_id (invite :id)
                                :now now
                                :form_hmac form-hmac})))

(defn verify-form-hmac
  [params]
  (constant-comp
    (params :hmac)
    (hmac (config :hmac-secret)
          (str (params :now) (params :token) (params :invite_id) (params :email)))))

;; invite by link

(defn make-open-invite-link
  "Create a link that will allow anyone with the link to register until it expires"
  [group-id expires]
  (let [nonce (random-nonce 20)
        expiry (->> (case expires
                      :day (t/days 1)
                      :week (t/weeks 1)
                      :month (t/months 1)
                      :never (t/years 1000))
                    (t/plus (t/now))
                    (c/to-long))
        mac (hmac (config :hmac-secret) (str nonce group-id expiry))]
    (str (config :site-url) "/invite?group-id=" group-id "&nonce=" nonce
         "&expiry=" expiry "&mac=" mac)))

(defn link-signup-page
  [group-id]
  (let [now (.getTime (java.util.Date.))
        group (group/group-by-id group-id)
        form-hmac (hmac (config :hmac-secret) (str now group-id))]
    (cljstache/render-resource "templates/link_signup.html.mustache"
                               {:now now
                                :form-hmac form-hmac
                                :group-id group-id
                                :group-name (group :name)})))

;; reset paswords

(defn request-reset
  [user]
  (let [secret (random-nonce 20)]
    (cache-set! (str (user :id)) secret)
    (mail/send!
      {:to (user :email)
       :subject "Password reset requested"
       :body {:text (str "A password reset was requested on "
                         (config :site-url) " for " (user :email) "\n\n"
                         "If this was you, follow this link to reset your password "
                         (config :site-url) "/reset?user=" (user :id) "&token=" secret
                         "\n\n"
                         "If this wasn't you, just ignore this email")
              :html (str "<html><body>"
                         "<p>"
                         "A password reset was requested on "
                         (config :site-url) " for " (user :email)
                         "</p>"
                         "<p>"
                         "If this was you, <a href=\""
                         (config :site-url) "/reset?user=" (user :id) "&token=" secret
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
        form-hmac (hmac (config :hmac-secret) (str now token (user :id)))]
    (cljstache/render-resource "templates/reset_page.html.mustache"
                               {:user_id (user :id)
                                :now now
                                :token token
                                :form_hmac form-hmac})))
