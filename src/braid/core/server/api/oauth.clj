(ns braid.core.server.api.oauth
  (:require
   [clojure.data.json :as json]
   [clojure.string :as string]
   [clojure.edn :as edn]
   [org.httpkit.client :as http]
   [braid.core.server.conf :refer [config]]
   [braid.core.server.crypto :as crypto]
   [braid.core.server.util :refer [map->query-str ->transit transit->form]])
  (:import
   (org.apache.commons.codec.binary Base64)))

(defn- config-oauth-get [key provider]
  (let [oauth-paramater-list (-> config :oauth-provider-list edn/read-string)]
    (get-in oauth-paramater-list [(keyword provider) key])))

(defn redirect-uri
  "This is a function instead of a var because we need to access config, which
  wouldn't be started at compile-time"
  [provider]
  (str (config :site-url) "/gateway/post-oauth/" provider))

(defn build-authorize-link
  [provider {:keys [register? group-id]}]
  (let [nonce (crypto/random-nonce 10)
        now (.getTime (java.util.Date. ))
        hmac (crypto/hmac (config :hmac-secret) (str nonce now register? group-id))
        state (-> (->transit {::nonce nonce
                              ::sent-at now
                              ::register? register?
                              ::group-id group-id
                              ::provider provider
                              ::hmac hmac})
                  Base64/encodeBase64
                  String.)]
    (str (config-oauth-get :auth-uri provider)
         (let [default-params {:client_id (config-oauth-get :client-id provider)
                               :state state}]
           (map->query-str (case provider
                             "google" (assoc default-params
                                             :response_type "code"
                                             :scope "email"
                                             :redirect_uri (redirect-uri "google"))
                             "github" (assoc default-params
                                             :scope "user:email"
                                             :redirect_uri (redirect-uri "github"))))))))

(defn exchange-token
  [code state provider]
  (let [info (transit->form (Base64/decodeBase64 state))]
    (when (and (map? info)
                                        ; TODO: use spec to validate state when we can use 1.9
               (crypto/hmac-verify {:secret (config :hmac-secret)
                                    :mac (info ::hmac)
                                    :data (->> [::nonce ::sent-at ::register? ::group-id]
                                               (map info)
                                               string/join)})
                                        ; Was the request from the last 30 minutes?
               (< 0
                  (- (.getTime (java.util.Date.)) (info ::sent-at))
                  (* 1000 60 30)))
      (let [default-params {:client_id (config-oauth-get :client-id provider)
                            :client_secret (config-oauth-get :client-secret provider)
                            :code code
                            :redirect_uri (redirect-uri provider)
                            :state state}
            resp @(http/post (config-oauth-get :access-token-uri provider)
                             {:headers {"Accept" "application/json"}
                              :form-params (case provider
                                             "google" (assoc default-params :grant_type "authorization_code")
                                             default-params)})]
        (when-let [parsed-resp (json/read-str (:body resp) :key-fn keyword)]
          (assoc parsed-resp
                 :braid.server.api/register? (info ::register?)
                 :braid.server.api/group-id (info ::group-id)))))))

(defn email-address
  [token provider]
  (let [resp @(http/get (config-oauth-get :email-uri provider)
                        {:headers (case provider
                                    "github" {"Authorization" (str "token " token)}
                                    "google" {"Authorization" (str "Bearer " token)})})
        body (json/read-str (:body resp) :key-fn keyword)]
    (case provider
      "github" (->> body
                    (filter #(and (:verified %) (:primary %)))
                    first
                    :email)
      "google" 
      (-> body :emails first :value))))
