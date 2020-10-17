(ns braid.chat.core
  (:require
    [braid.base.api :as base]))

(defn init! []
  #?(:clj
     ;; TODO some of these might better belong elsewhere
     (doseq [k [:api-domain
                :asana-client-id
                :asana-client-secret
                :aws-access-key
                :aws-domain
                :aws-region
                :aws-secret-key
                :db-url
                :embedly-key
                :environment
                :github-client-id
                :github-client-secret
                :hmac-secret
                :mailgun-domain
                :mailgun-password
                :site-url]]
       (base/register-config-var! k))))

