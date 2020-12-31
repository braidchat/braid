(ns braid.core.hooks
  "Provides a way to register atoms that can later all be reset to their initial values.
   This enables changes to Braid modules to be picked up in a reloaded workflow (ie. figwheel).

  On the client-side:
  Figwheel is configured to call (braid.core.client.desktop.core/reload!),
  which calls (braid.core.modules/init!), which calls (reset-all!)

  On the server-side:
  (braid.core.modules/init!) is called once, when the server is started (in braid.core)."
  (:require
    [braid.core.common.util :as util]))

(defonce atoms (atom {}))

(defn register!
  "Takes an atom and adds it to a central registry so that it can be later reset to its initial value.
   Only the first registration of an atom is stored.
   Always returns the given atom."
  ([a data-spec]
   (when-not (contains? @atoms a)
     (swap! atoms assoc a @a))
   (when data-spec
     (set-validator! a (fn [x]
                         ;; use assert, to elide the code in prod
                         (assert (util/valid? data-spec x))
                         ;; assert returns nil
                         ;; but validator expects true
                         true)))
   a)
  ([a]
   (register! a nil)))

(defn reset-all!
  "Resets all registered atoms to their initial state."
  []
  (doseq [[a initial-state] @atoms]
    (reset! a initial-state)))
