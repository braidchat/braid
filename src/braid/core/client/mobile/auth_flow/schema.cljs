(ns braid.core.client.mobile.auth-flow.schema
  (:require
    [clojure.spec.alpha :as s]))

(def init-state
  {:auth-flow {:method nil
               :stage nil}})

(def MobileAuthFlowAppState
  {:auth-flow {:method (s/spec #{:login :register})
               :stage (s/spec #{:email :password})}})
