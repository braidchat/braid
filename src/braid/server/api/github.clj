(ns braid.server.api.github
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [braid.server.conf :refer [config]]
            [braid.server.cache :as cache]
            [braid.server.crypto :as crypto]
            [braid.server.db :as db]
            [braid.server.util :refer [map->query-str ->transit transit->form]]
            [braid.server.identicons :as identicons])
  (:import org.apache.commons.codec.binary.Base64))

(defn redirect-uri
  "This is a function instead of a var because we need to access config, which
  wouldn't be started at compile-time"
  []
  (let [domain (config :api-domain)
        proto (str "http"
                   (when-not (string/starts-with? domain "localhost") "s")
                   "://")]
    (str proto domain "/oauth/github")))

(defn build-authorize-link
  [register?]
  (let [nonce (crypto/random-nonce 10)
        now (.getTime (java.util.Date.))
        hmac (crypto/hmac (config :hmac-secret) (str nonce now register?))
        state (-> (->transit {::nonce nonce
                              ::sent-at now
                              ::register? register?
                              ::hmac hmac})
                  Base64/encodeBase64
                  String.)]
    (str "https://github.com/login/oauth/authorize?"
         (map->query-str
           {:client_id (config :github-client-id)
            :scope "user:email"
            :redirect_uri (redirect-uri)
            :state state}))))

(defn exchange-token
  [code state]
  (let [info (transit->form (Base64/decodeBase64 state))]
    (when (and (map? info)
            ; TODO: use spec to validate state when we can use 1.9
            (crypto/hmac-verify {:secret (config :hmac-secret)
                                 :mac (info ::hmac)
                                 :data (->> [::nonce ::sent-at ::register?]
                                            (map info)
                                            string/join)})
            ; Was the request from the last 30 minutes?
            (< 0
               (- (.getTime (java.util.Date.)) (info ::sent-at))
               (* 1000 60 30)))
      (let [resp @(http/post "https://github.com/login/oauth/access_token"
                             {:headers {"Accept" "application/json"}
                              :form-params
                              {:client_id (config :github-client-id)
                               :client_secret (config :github-client-secret)
                               :code code
                               :redirect_uri (redirect-uri)
                               :state state}})]
        (json/read-str (:body resp) :key-fn keyword)))))

(defn email-address
  [token]
  (let [resp @(http/get "https://api.github.com/user/emails"
                {:headers {"Authorization" (str "token " token)}})
        emails (json/read-str (:body resp) :key-fn keyword)]
    (->> emails
         (filter #(and (:verified %) (:primary %)) )
         first
         :email)))

(defn login
  [token]
  (when-let [email (email-address token)]
    (db/user-with-email email)))

(defn register
  [token]
  (when-let [email (email-address)]
    (let [id (db/uuid)
          avatar (identicons/id->identicon-data-url id)]
      (db/create-oauth-user! {:id id :email email :avatar avatar}))))
