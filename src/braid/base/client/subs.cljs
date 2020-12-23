(ns braid.base.client.subs
  (:require
   [re-frame.core :refer [reg-sub reg-sub-raw]]))

(defn register-subs!
  [sub-map]
  (doseq [[k f] sub-map]
    (reg-sub k f)))

(defn register-subs-raw!
  [sub-map]
  (doseq [[k f] sub-map]
    (reg-sub-raw k f)))
