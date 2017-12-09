(ns braid.core.hooks
  (:require [robert.hooke :as hooke]))


(defmacro def-data-hook
  [hook-name base-val]
  `(do
     ;; To hook, the function needs to take at least one argument so we can change it
     (defn hidden# [x#] x#)
     (defn ~hook-name
       []
       (hidden# ~base-val))
     (alter-meta! (var ~hook-name) assoc ::hook true ::hook-fn (var hidden#))))


(defmacro def-data-hook-extension
  "Add data to the hook point.
  The provided `body` should return data that will be added via `into`
  to the accumulated data."
  [hook ext-name & body]
  `(do
     (defn ~ext-name
       [f# data#]
       (f# (into data# (do ~@body))))

     (hooke/add-hook (::hook-fn (meta (var ~hook))) (var ~ext-name))))


(comment

  (def-data-hook schema [])

  (::hook-fn (meta #'schema))

  (def-data-hook-extension schema quests-schema
    [[{:db/ident :quests/id}]])

  (def-data-hook-extension schema bots-schema
    [[{:db/ident :bots/id}]])

  (schema)

  )
