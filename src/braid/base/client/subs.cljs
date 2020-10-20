(ns braid.base.client.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(defn register-subs!
  [sub-map]
  (doseq [[k f] sub-map]
    (reg-sub k f)))
