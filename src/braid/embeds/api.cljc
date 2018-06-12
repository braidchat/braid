(ns braid.embeds.api
  (:require
    [braid.core.api :as core]
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
       {:pre [(fn? handler)
              (vector? styles)
              (number? priority)]}
       (swap! impl/embed-engines conj embed)
       (core/register-styles! [:.embed styles]))))
