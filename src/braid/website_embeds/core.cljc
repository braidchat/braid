(ns braid.website-embeds.core
  "If a message contains a link, displays a generic website embed"
  (:require
    [braid.core.api :as core]
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.embeds.api :as embeds]
          [braid.website-embeds.styles :as styles]
          [braid.website-embeds.views :as views]]
         :clj
         [[braid.website-embeds.link-extract :as link-extract]])))

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
            (let [url (get-in request [:params :url])]
              {:body (link-extract/extract url)}))]))))
