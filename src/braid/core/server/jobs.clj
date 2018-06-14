(ns braid.core.server.jobs
  (:require
   [braid.core.hooks :as hooks]
   [braid.core.server.scheduler :refer [scheduler]]
   [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
   [clojurewerkz.quartzite.schedule.cron :as cron]
   [clojurewerkz.quartzite.scheduler :as qs]
   [clojurewerkz.quartzite.triggers :as t]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]))

(defonce daily-module-jobs (hooks/register! (atom [])))

(defjob DailyModulesJob
  [ctx]
  (timbre/debugf "Starting daily modules job")
  (doseq [job @daily-module-jobs]
    (try
      (job)
      (catch Exception e
        (timbre/errorf "Error running job %s: %s" job e)))))

(defn daily-modules-job
  []
  (j/build
    (j/of-type DailyModulesJob)
    (j/with-identity (j/key "jobs.daily-modules.1"))))

(defn daily-modules-trigger
  []
  (t/build
    (t/with-identity (t/key "triggers.daily-modules"))
    (t/start-now)
    (t/with-schedule
      (cron/schedule
        (cron/daily-at-hour-and-minute 2 30)))))

(defn register-daily-job!
  [job-fn]
  (swap! daily-module-jobs conj job-fn))

(defn add-jobs
  [scheduler]
  (qs/schedule scheduler (daily-modules-job) (daily-modules-trigger)))

(defn remove-jobs
  [scheduler]
  (qs/delete-job scheduler (j/key "jobs.daily-modules.1")))

(defstate jobs
  :start (add-jobs scheduler)
  :stop (remove-jobs scheduler))
