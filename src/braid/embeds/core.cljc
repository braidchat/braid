(ns braid.embeds.core
  "Allows for extending Braid with embed handlers, which can display inline content after a message, based on the content of the message"
  (:require
    [braid.core.core :as core]
    #?@(:cljs
         [[braid.embeds.impl :as impl]
          [braid.embeds.styles :as styles]])))

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

(defn init! []
  #?(:cljs
     (do
       (core/register-styles! styles/embed)
       (core/register-post-message-view! impl/embed-view))))
