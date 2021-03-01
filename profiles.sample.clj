; Rename this file to profiles.clj
; Run `lein with-profile +braid repl`

{:braid
 {:env
  {;; for invite emails:
   :email-host "smtp.mailgun.org"
   :email-user "myuser@site.com"
   :email-password "my_email_password"
   :email-port 587
   :email-secure "tls"
   :email-from "noreply@braid.chat"
   :site-url "http://localhost:5555"
   :hmac-secret "foobar"
   ;; for avatar and file uploads
   :aws-bucket "braid-bucket"
   :aws-region "us-east-1"
   :aws-access-key "my_aws_key"
   :aws-secret-key "my_aws_secret"
   ;; for github login
   :github-client-id "..."
   :github-client-secret "..."
   :google-maps-api-key ""
   }
  }
 }
