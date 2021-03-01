(ns braid.core.server.mail
  "Sending email"
  (:require
   [braid.base.conf :as conf]
   [postal.core :as postal]))

(defn- ->int [s]
  (try (Long. s)
       (catch java.lang.NumberFormatException _ nil)))

(defn send!
  "Send an email message via SMTP using the server information in config.

  `body` can be a string, which will sent as plain text or a map of
  `:text` and `:html`, which will send a multipart/alternative message.

  Uses config variables:
  :email-host :email-user :email-password :email-port :email-from
  :email-secure can be the string `ssl` or `tls`
  "
  [{:keys [subject body to from]}]
  (postal/send-message
    (cond-> {:host (conf/config :email-host)
             :user (conf/config :email-user)
             :pass (conf/config :email-password)}
      (->int (conf/config :email-port)) (assoc :port (->int (conf/config :email-port)))
      (= "tls" (conf/config :email-secure)) (assoc :tls true)
      (= "ssl" (conf/config :email-secure)) (assoc :ssl true))
    {:from (or from (conf/config :email-from))
     :to to
     :subject subject
     :body (if (string? body)
             body
             [:alternative
              {:type "text/plain"
               :content (:text body)}
              {:type "text/html"
               :content (:html body)}])}))
