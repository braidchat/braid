(ns braid.core.client.state.fx.redirect
  (:require
    [braid.base.client.router :as router]))

(defn redirect-to [route]
  (when route (router/go-to route)))
