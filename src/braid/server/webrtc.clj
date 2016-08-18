(ns braid.server.webrtc
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [environ.core :refer [env]]))

(defn get-ice-servers []
  (let [response @(http/request
                    {:url "https://api.twilio.com/2010-04-01/Accounts/AC01773bdc00cc61649a19c84303b85c82/Tokens.json"
                     :method :post
                     :basic-auth [(env :twilio-key)
                                  (env :twilio-secret)]})
        ice-servers (-> response
                        (get :body)
                        (json/read-str :key-fn keyword)
                        (get :ice_servers))]
    ice-servers))
