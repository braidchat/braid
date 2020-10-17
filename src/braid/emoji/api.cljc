(ns braid.emoji.api
  (:require
    [braid.core.api :as core]
    [braid.base.api :as base]
    #?@(:cljs
         [[braid.emoji.client.lookup :as lookup]])))

#?(:cljs
   (do
     (defn register-emoji!
       "Adds emoji, which will show up in autocomplete and in messages.

       Takes a map with three optional keys:

        :shortcode-lookup
           a 0-arity function that returns a map of shortcode->emoji-meta-map
           ex.  (fn []
                  {\":foobar:\" {:src \"https://example.com/foobar.png\"
                                 :class \"example-emoji\"}})
           the keys must have colons as prefixes and suffixes
           these will show up in autocomplete when the user types in a colon or bracket
           in messages and autocomplete, the :src and :class will be used in an image element

        :ascii-lookup
          a 0-arity function that returns a map of string->emoji-meta-map
          ex.  (fn []
                {\":)\" {:src \"https://example.com/smiley.png\"
                         :class \"example-emoji\"})
          these will not be used in autocomplete, but will be used in message

        :styles
          garden style rules for custom styling emoji"
       [{:keys [shortcode-lookup ascii-lookup styles]}]
       (when shortcode-lookup
         (swap! lookup/shortcode-fns conj shortcode-lookup))
       (when ascii-lookup
         (swap! lookup/ascii-fns conj ascii-lookup))
       (when styles
         (base/register-styles! styles)))))
