(ns chat.server.invite
  (:require [org.httpkit.client :as http]
            [taoensso.carmine :as car]
            [clojure.string :as string]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]])
  (:import java.security.SecureRandom
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

; same as conf in handler, but w/e
(def redis-conn {:pool {}
                 :spec {:host "127.0.0.1"
                        :port 6379}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn make-invite-link
  [invite]
  (let [secret-nonce (random-nonce 20)]
    (wcar* (car/set (str (invite :id)) secret-nonce)
           (car/expire (str (invite :id)) (* 3600 2)))
    (str (env :site-url)
         "/accept?tok=" secret-nonce
         "&?invite=" (invite :id))))

(defn verify-invite-nonce
  "Verify that the given nonce is valid for the invite"
  [invite nonce]
  (if-let [stored-nonce (wcar* (car/get (str (invite :id))))]
    (if (constant-comp stored-nonce nonce)
      (do (wcar* (car/del (str (invite :id))))
          true)
      {:error "Invalid token"})
    (do (timbre/warnf "Expired nonce %s for invite %s" nonce invite)
        {:error "Expired token"})))

(defn invite-message
  [invite]
  (let [accept-link ""]
    {:text (str (invite :inviter-email) " has invited to join the " (invite :group-name)
                " group on chat.leanpixel.com.\n\n"
                "Go to " accept-link " to accept.")
     :html (str "<html><body>"
                (invite :inviter-email) " has invited to join the " (invite :group-name)
                " group on <a href=\"https://chat.leanpixel.com\">chat.leanpixel.com.</a>"
                "<br>"
                "<a href=\"" accept-link "\">Click here</a> to accept."
                "</body></html>")}))

(defn send-invite
  [invite]
  (http/post "https://api.mailgun.net/v3/chat.leanpixel.com/messages"
             {:basic-auth ["api" (env :mailgun-password)]
              :form-params (merge {:to (invite :invitee-email)}
                                  (invite-message invite))}))
