(ns braid.core.hooks
  (:require [robert.hooke :as hooke]))


(defmacro def-data-hook
  [hook-name [& args] base-val]
  `(do
     ;; To hook, the function needs to take at least one argument so we can change it
     (defn hidden# [x# & _#] x#)
     (defn ~hook-name
       [~@args]
       (hidden# ~base-val ~@args))
     (alter-meta! (var ~hook-name) assoc ::data-hook true ::hook-fn (var hidden#))))

; TODO: do we need to have the name? could be useful for deactivating hooks later
(defmacro def-data-hook-extension
  "Add data to the hook point.
  The provided `body` should return data that will be added via `into`
  to the accumulated data."
  [hook ext-name [& args] & body]
  `(do
     (assert (::data-hook (meta (var ~hook)))
             (str "Attempting to extend a non-data hook var " '~hook))

     (defn ~ext-name
       [f# data# ~@args]
       (f# (into data# (do ~@body)) ~@args))

     (hooke/add-hook (::hook-fn (meta (var ~hook))) (var ~ext-name))))

(comment

  (def-data-hook schema []
    [])

  (::hook-fn (meta #'schema))

  (def-data-hook-extension schema quests-schema
    []
    [{:db/ident :quests/id}])

  (def-data-hook-extension schema bots-schema
    []
    [{:db/ident :bots/id}])

  (schema)

  (def-data-hook init-data [user-id]
    {:id user-id})

  (def-data-hook-extension init-data quests-init
    [user-id]
    {:quests/for user-id
     :other-thing 123})

  (init-data 'user-id)
  )
