(ns braid.test.fixtures.conf
  (:require
   [braid.core.modules :as modules]
   [braid.base.conf :as conf]
   [mount.core :as mount]))

(defn start-config
  [t]
  (modules/init! modules/default)
  (-> (mount/only #{#'conf/config})
      (mount/with-args {:port 0})
      (mount/start))
  (t)
  (mount/stop))
