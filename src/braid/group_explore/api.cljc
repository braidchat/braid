(ns braid.group-explore.api
#?(:cljs
   (:require
    [braid.group-explore.views :as views])))

#?(:cljs
   (defn register-link!
     [{:keys [title url] :as link}]
     (swap! views/group-explore-links conj link)))
