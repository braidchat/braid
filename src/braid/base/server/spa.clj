(ns braid.base.server.spa
  (:require
    [cljstache.core :as cljstache]
    [braid.lib.s3 :as s3]
    [braid.core.hooks :as hooks]
    [braid.base.conf :refer [config]]
    [braid.lib.digest :as digest]))

(def additional-script-dataspec
  (fn [tag]
    (or (fn? tag)
        (and (map? tag)
          (or (:src tag) (:body tag))))))

(defonce additional-scripts
  (hooks/register! (atom []) [additional-script-dataspec]))

(defn init-script [client]
  (str "braid.core.client." client ".core.init();"))

(defn get-html [client vars]
  (let [prod-js? (= "prod" (config :environment))]
    (cljstache/render-resource
      (str "public/" client ".html")
      (merge {:prod prod-js?
              :dev (not prod-js?)
              :algo "sha256"
              :init_script (init-script client)
              :js (if prod-js?
                    (str (digest/from-file (str "public/js/prod/" client ".js")))
                    (str (digest/from-file (str "public/js/dev/" client ".js"))))
              :basejs (when prod-js?
                        (str (digest/from-file (str "public/js/prod/base.js"))))
              :api_domain (config :api-domain)
              :asset_domain (s3/s3-host config)
              :extra_scripts
              (->> @additional-scripts
                   (map (fn [s] (if (fn? s) (s) s)))
                   (map (fn [s]
                          {:script (if-let [src (:src s)]
                                      (str "<script src=\"" src "\"></script>")
                                      (str "<script>\n" (s :body) "\n</script>"))})))}
             vars))))
