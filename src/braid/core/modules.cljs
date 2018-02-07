(ns braid.core.modules
  (:require-macros [braid.core.module-helpers :refer [gen-module-loads]])
  (:require [braid.core.modules-dummy]))

(defn init!
  []
  (gen-module-loads))
