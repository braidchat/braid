(ns chat.client.schema
  (:require [cljs-uuid-utils.core :as uuid]))


(defn make-message [data]
  {:id (or (data :id) (uuid/make-random-squuid))
   :content (or (data :content) nil)
   :thread-id (or (data :thread-id) (uuid/make-random-squuid))
   :user-id (or (data :user-id) nil)
   :created-at (or (data :created-at) (js/Date.))})

(defn make-tag [data]
  {:id (or (data :id) (uuid/make-random-squuid))
   :name (data :name)})
