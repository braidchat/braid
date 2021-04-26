(ns braid.base.server.socket
  (:require
    [mount.core :as mount :refer [defstate]]
    [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
    [taoensso.sente.packers.transit :as sente-transit]
    [taoensso.sente :as sente]))

(let [packer (sente-transit/get-transit-packer :json)
      {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! (get-sch-adapter)
                                  {:user-id-fn
                                   (fn [ob] (or (get-in ob [:session :user-id])
                                               (get-in ob [:session :fake-id])))
                                   :simple-auto-threading? true
                                   :csrf-token-fn nil
                                   :ws-kalive-ms 30000
                                   :packer packer})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv)
  (def chsk-send!                    send-fn)
  (def connected-uids                connected-uids))
