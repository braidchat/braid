(ns braid.client.mobile.auth-flow.schema
  (:require [schema.core :as s :include-macros true]))

(def init-state
  {:auth-flow {:method nil
               :stage nil}})

(def MobileAuthFlowAppState
  {:auth-flow {:method (s/enum :login :register)
               :stage (s/enum :email :password)}})
