(ns braid.embeds.api
  (:require
    [braid.base.api :as base]
    [braid.core.common.util :as util]
    #?@(:cljs
         [[braid.embeds.impl :as impl]])))

#?(:cljs
   (do
     (defn register-embed!
       "Expects a map with:
         {:handler ___ - a fn that will return either nil or a vector of [view & args]
          :priority ___ - a number, higher means takes precedence before lower priority embed handlers
          :styles ___ - garden styles}

       The handler fn is passed a map:
         {:urls [\"http://example.org\"
                 \"http://example.org/2\"]}}"
       [{:keys [handler styles priority] :as embed}]
       {:pre [(util/valid? impl/embed-engine-dataspec embed)]}
       (swap! impl/embed-engines conj embed)
       (when styles
         (base/register-styles! [:.embed styles])))))
