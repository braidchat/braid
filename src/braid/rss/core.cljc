(ns braid.rss.core
  "Extension to post updates from RSS feeds as messages in a given group"
  (:require
   [braid.core.api :as core]
   #?@(:clj
       [[braid.rss.server.db :as db]])))

(defn init!
  []
  #?(:clj
     (do
       (core/register-db-schema! db/schema))))
