(ns braid.core.modules-dummy
  "This module just exists to generate the `require` statements to
  load the necessary namespaces for module linking in
  `braid.core.modules` (cljs) to work.
  This is a separate module instead of just happening in
  `braid.core.modules` because otherwise the code tries to run before
  the modules have loaded -- a consequence of how `require` worksl in
  Clojurescript, I suppose."
  (:require-macros [braid.core.module-helpers :refer [gen-module-requires]]))

(gen-module-requires)
