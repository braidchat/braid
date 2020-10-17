(ns braid.base.api
  (:require
    #?@(:cljs
         [[braid.base.client.events]
          [braid.base.client.subs]]
         :clj
         [[braid.base.server.jobs]])))

#?(:cljs
   (do
     (defn register-events!
       "Registers multiple re-frame event handlers, as if passed to reg-event-fx.

       Expects a map of event-keys to event-handler-fns."
       [event-map]
       {:pre [(map? event-map)
              (every? keyword? (keys event-map))
              (every? fn? (vals event-map))]}
       (braid.base.client.events/register-events! event-map))

     (defn register-event-listener!
       "Register a function to intercept re-frame events."
       [f]
       {:pre [(fn? f)]}
       (swap! braid.base.client.events/event-listeners conj f))

     (defn register-subs!
       "Registers multiple re-frame subscription handlers, as if passed to reg-sub.

       Expects a map of sub-keys to sub-handler-fns."
       [sub-map]
       {:pre [(map? sub-map)
              (every? keyword? (keys sub-map))
              (every? fn? (vals sub-map))]}
       (braid.base.client.subs/register-subs! sub-map)))

   :clj
   (do
     (defn register-daily-job!
       "Add a recurring job that will run once a day. Expects a zero-arity function."
       [job-fn]
       {:pre [(fn? job-fn)]}
       (braid.base.server.jobs/register-daily-job! job-fn))))
