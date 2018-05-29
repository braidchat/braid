; Rename this file to profiles.clj
; Run `lein with-profile +braid repl`

{:braid
 {:env
  { ;; for invite emails:
   :mailgun-domain "braid.mysite.com"
   :mailgun-password "my_mailgun_key"
   :site-url "http://localhost:5555"
   :hmac-secret "foobar"
   ;; for avatar and file uploads
   :aws-domain "braid.mysite.com"
   :aws-access-key "my_aws_key"
   :aws-secret-key "my_aws_secret"
   :s3-upload-key "key-for-uploading"
   :s3-upload-secret "secret-for-uploading"
   ;; for link info extraction
   :embedly-key "embedly api key"
   ;; for oauth
   {:oauth-provider-list {:github {:auth-uri "https://github.com/login/oauth/authorize?"
                                   :access-token-uri "https://github.com/login/oauth/access_token"
                                   :client-id ""
                                   :client-secret ""
                                   :email-uri "https://api.github.com/user/emails"}
                          :google {:auth-uri "https://accounts.google.com/o/oauth2/v2/auth?"
                                   :access-token-uri "https://www.googleapis.com/oauth2/v4/token"
                                   :client-id ""
                                   :client-secret ""
                                   :email-uri "https://www.googleapis.com/plus/v1/people/userId"}}}}}}
