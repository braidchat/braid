(ns braid.client.calls.state
  (:require [schema.core :as s :include-macros true]
            [braid.common.schema :as app-schema]))

(def init-state
  {:calls {}})

(def CallsState
  {:calls {s/Uuid app-schema/Call}})
