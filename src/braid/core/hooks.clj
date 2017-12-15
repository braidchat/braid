(ns braid.core.hooks
  "Hooks as an extnesion interface to braid.
  Consumers can define hooks and providers can extend those hooks to add functionality"
  (:require [robert.hooke :as hooke]))


(defmacro def-data-hook
  "Define a 'data' hook named `hook-name`. That is, a collection that
  can be added to by providers. The initial value is `base-val` and
  can be added to by providers using `def-data-hook-extension`."
  [hook-name [& args] base-val]
  `(do
     ;; To hook, the function needs to take at least one argument so we can change it
     (defn hidden# [x# & _#] x#)
     (defn ~hook-name
       [~@args]
       (hidden# ~base-val ~@args))
     (alter-meta! (var ~hook-name) assoc ::data-hook true ::hook-fn (var hidden#))))

(defmacro def-data-hook-extension
  "Add data to the hook point `hook` by creating a new function named `ext-name`.
  The provided `body` will have access to `args` and should return
  data that will be added via `into` to the accumulated data."
  ;; TODO: do we need to have the name? could be useful for deactivating hooks later
  [hook ext-name [& args] & body]
  `(do
     (assert (::data-hook (meta (var ~hook)))
             (str "Attempting to extend a non-data hook var " '~hook))
     ;; TODO: validate that args here matches the args of def-data-hook?

     (defn ~ext-name
       [f# data# ~@args]
       (f# (into data# (do ~@body)) ~@args))

     (hooke/add-hook (::hook-fn (meta (var ~hook))) (var ~ext-name))))

;; Example of usage
(comment

  (def-data-hook schema []
    [])

  (def-data-hook-extension schema quests-schema
    []
    [{:db/ident :quests/id}])

  (def-data-hook-extension schema bots-schema
    []
    [{:db/ident :bots/id}])

  (schema)
  ;; [{:db/ident :bots/id} {:db/ident :quests/id}]

  (def-data-hook init-data [user-id]
    {:id user-id})

  (def-data-hook-extension init-data quests-init
    [user-id]
    {:quests/for user-id
     :other-thing 123})

  (init-data 'the-user-id)
  ;; {:id the-user-id, :quests/for the-user-id, :other-thing 123}

  )
