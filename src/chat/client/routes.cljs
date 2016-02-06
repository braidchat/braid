(ns chat.client.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [chat.client.store :as store]))

(defroute index-path "/" {}

  )

(defroute page-path "/:group-id/:page-id" [group-id page-id]
  (store/set-group-and-page! (UUID. group-id nil) (keyword page-id)))

(defroute other-path "/:page-id" [page-id]
  (store/set-group-and-page! nil (keyword page-id)))

