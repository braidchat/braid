(ns braid.base.client.events
  (:require
    [braid.base.client.state :refer [reg-event-fx]]
    [braid.core.client.desktop.notify :as notify]
    [braid.core.client.state.fx.dispatch-debounce :as fx.debounce]
    [braid.core.client.state.fx.redirect :as fx.redirect]
    [braid.base.client.socket :as socket]
    [braid.lib.xhr :refer [edn-xhr]]
    [braid.core.hooks :as hooks]
    [re-frame.core :as re-frame :refer [reg-fx]]))

(defonce event-listeners
  (hooks/register! (atom []) [fn?]))

(re-frame/add-post-event-callback
  (fn [event _]
    (doseq [handler @event-listeners]
      (handler event))))

(defn register-events!
  [event-map]
  (doseq [[k f] event-map]
    (reg-event-fx k f)))

(reg-fx :dispatch-debounce
        fx.debounce/dispatch)

(reg-fx :stop-debounce
        fx.debounce/stop)

; TODO: handle callbacks declaratively too?
(reg-fx :websocket-send (fn [args] (when args (apply socket/chsk-send! args))))

; TODO: handle callbacks declaratively too?
(reg-fx :edn-xhr (fn [args] (edn-xhr args)))

(reg-fx :redirect-to fx.redirect/redirect-to)

(reg-fx :window-title (fn [title] (when title (set! (.-title js/document) title))))

(reg-fx :notify notify/notify)

(reg-fx :confirm (fn [{:keys [prompt on-confirm on-cancel]}]
                   (if (js/confirm prompt)
                     (on-confirm)
                     (when on-cancel (on-cancel)))))

(reg-fx :command
        (fn
          [[event-id event-params
            {:keys [on-success on-error]
             :or {on-success (constantly nil)
                  on-error (fn [err]
                             (.error js/console "tada.rpc request error: " (pr-str err)))}} :as event]]
          (when event
            (socket/chsk-send!
              [event-id event-params]
              1000
              (fn [reply]
                (cond
                  ;; websocket error
                  (#{:chsk/closed :chsk/timeout :chsk/error} reply)
                  (on-error reply)

                  ;; tada/cqrs error
                  (:cqrs/error reply)
                  (on-error (:cqrs/error reply))

                  :else
                  (on-success reply)))))))

(defonce initial-user-data-handlers
  (hooks/register! (atom []) [fn?]))
