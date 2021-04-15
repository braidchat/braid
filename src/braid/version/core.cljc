(ns braid.version.core
  (:require
    [braid.base.api :as base]
    #?@(:clj
         [[braid.base.conf :refer [config]]])))

(defn init! []

  #?(:clj
     (do
       (base/register-config-var! :version :optional [:string])

       (base/register-additional-script!
         {:body
          ;; can't just include a string here
          ;; b/c config is not ready at "compile" time
          ;; needs to be at run time
          (reify Object
            (toString [_]
              (str "window.version='" (config :version) "';")))}))

     :cljs
     (do
       (base/register-root-view!
         (fn []
           [:div.version
            js/window.version]))

       (base/register-styles!
         [:.main>.version
          {:opacity 0
           :position "absolute"
           :bottom 0
           :right 0}

          [:&:hover
           {:opacity 1}]]))))
