(ns braid.core.module-helpers
  "Helpers for the Braid module system.
  - The `defhook` macro, which Clojure & Clojurescript should use to
    define extension points for modules")

(defmacro defhook
  "Macro to generate a module extension point.
  `:writer` will be a function name that should be in the `:provides`
  section of the module.edn.
  `:reader` will be deref-able and will give a collection of the
  values passed to the writer.

  By default, the values will be `conj`-ed into a vector, but this can
  be customized with the `:initial-value` and `:add-fn` arguments."
  [& {:keys [reader writer initial-value add-fn]
      :or {initial-value [] add-fn `conj}}]
  (assert (symbol? reader))
  (assert (symbol? writer))
  `(do
     (defonce ~reader (atom ~initial-value))
     (defn ~writer [v#]
       (swap! ~reader ~add-fn v#))))
