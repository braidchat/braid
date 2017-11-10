(ns braid.core.modules
  (:require
    [braid.client.ui.views.autocomplete]
    [braid.emoji.core]))

(defn init []
  (braid.emoji.core/init))
