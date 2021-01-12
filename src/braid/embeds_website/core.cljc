(ns braid.embeds-website.core
  "If a message contains a link, displays a generic website embed"
  (:require
   [braid.base.api :as base]
   [braid.embeds.api :as embeds]
   #?@(:cljs
       [[braid.embeds-website.styles :as styles]
        [braid.embeds-website.views :as views]]
       :clj
       [[braid.embeds-website.link-extract :as link-extract]]))
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
