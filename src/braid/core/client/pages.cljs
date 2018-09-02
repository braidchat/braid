(ns braid.core.client.pages
  (:require
    [spec-tools.data-spec :as ds]
    [braid.core.hooks :as hooks]))

(def page-dataspec
  {:key keyword?
   :view fn?
   (ds/opt :on-load) fn?
   (ds/opt :on-exit) fn?
   (ds/opt :styles) vector?})

(defonce pages
  (hooks/register! (atom {}) {keyword? page-dataspec}))

(defn on-load! [page-id page]
  (when-let [f (get-in @pages [page-id :on-load])]
    (f page)))

(defn on-exit! [page-id page]
  (when-let [f (get-in @pages [page-id :on-exit])]
    (f page)))

(defn get-view [page-id]
  (get-in @pages [page-id :view]))
