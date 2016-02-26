(ns chat.server.extensions.asana
  (:require [clojure.edn :as edn]
            [taoensso.carmine :as car]
            [environ.core :refer [env]]
            [chat.server.db :as db]
            [chat.server.cache :refer [cache-set! cache-get cache-del! random-nonce]])
  (:import org.apache.commons.codec.binary.Base64
           (com.asana Client OAuthApp)
           (com.asana.models Project Task)))

(def redirect-uri (str (env :site-url) "/extension-oauth"))
(def client-id (env :asana-client-id))
(def client-secret (env :asana-client-secret))

(defn auth-url
  [extension-id]
  (let [nonce (random-nonce 20)
        info (-> {:extension-id extension-id
                  :nonce nonce}
                 pr-str
                 (.getBytes)
                 (Base64/encodeBase64)
                 String.)]
    (cache-set! (str extension-id) nonce)
    (.. (Client/oauth (OAuthApp. client-id client-secret redirect-uri))
        -dispatcher
        -app
        (getAuthorizationUrl info))))

(defn exchange-token
  [state code]
  (let [{ext-id :extension-id sent-nonce :nonce} (-> state .getBytes
                                                     Base64/decodeBase64 String.
                                                     edn/read-string)]
    (when-let [stored-nonce (cache-get (str ext-id))]
      (when (= stored-nonce sent-nonce)
        (let [dispatcher (.. (Client/oauth (OAuthApp. client-id client-secret
                                                      redirect-uri))
                             -dispatcher -app)
              token (.fetchToken dispatcher code)]
          (db/with-conn (db/save-extension-token! ext-id token)))))))
