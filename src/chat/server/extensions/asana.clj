(ns chat.server.extensions.asana
  (:require [taoensso.carmine :as car]
            [environ.core :refer [env]]
            [chat.server.db :as db]
            [chat.server.cache :refer [cache-set! cache-get cache-del! random-nonce]])
  (:import org.apache.commons.codec.binary.Base64
           (com.asana Client OAuthApp)
           (com.asana.models Project Task)))

(def redirect-uri (str (env :site-url) "/extension-auth"))

(defn auth-url
  [extension-id ^String client-id ^String client-secret]
  (let [state (random-nonce 20)
        info (-> {:extension-id extension-id
                  :client-id client-id
                  :client-secret client-secret}
                 pr-str
                 (.getBytes)
                 (Base64/encodeBase64)
                 String.)]
    (cache-set! state info)
    (.. (Client/oauth (OAuthApp. client-id client-secret redirect-uri))
        -dispatcher
        (getAuthorizationURL state))))

(defn exchange-token
  [state code]
  (when-let [info (cache-get state)]
    (let [{:keys [client-id client-secret extension-id]} (-> info .getBytes
                                                             Base64/decodeBase64 String.)
          dispatcher (.-dispatcher
                       (Client/oauth (OAuthApp. client-id client-secret redirect-uri)))
          token (.fetchToken dispatcher code)]
      (db/with-conn (db/save-extension-token!))
      )))
