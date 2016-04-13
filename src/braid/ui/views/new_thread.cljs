(ns braid.ui.views.new-thread
  (:require [cljs-uuid-utils.core :as uuid]))

(defn new-thread-view [opts]
  [thread-view (merge {:id (uuid/make-random-squuid)
                       :new? true
                       :tag-ids []
                       :mentioned-ids []
                       :messages []}
                      opts)])
