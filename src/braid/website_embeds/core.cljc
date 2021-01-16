(ns braid.website-embeds.core
  "If a message contains a link, displays a generic website embed"
  (:require
   [braid.base.api :as base]
   [braid.embeds.api :as embeds]
   #?@(:cljs
       [[braid.website-embeds.styles :as styles]
        [braid.website-embeds.views :as views]]
       :clj
       [[braid.website-embeds.link-extract :as link-extract]]))
  #?(:clj
     (:import
      (java.net URLDecoder))))

(defn init! []
  #?(:cljs
     (do
       (embeds/register-embed!
         {:handler views/handler
          :styles styles/styles
          :priority -1}))
     :clj
     (do
       (base/register-public-http-route!
         [:get "/extract"
          (fn [request]
            (let [url (URLDecoder/decode (get-in request [:params :url]))]
              {:body (link-extract/extract url)}))]))))
