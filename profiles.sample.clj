; Rename this file to profiles.clj
; Run `lein with-profile +braid repl`

{:braid
 {:env
  {;; for invite emails:
   :mailgun-domain "braid.mysite.com"
   :mailgun-password "my_mailgun_key"
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
