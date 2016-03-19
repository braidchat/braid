(ns chat.server.email-digest
  (:require [clojurewerkz.quartzite
             [triggers :as t]
             [jobs :as j :refer [defjob]]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [chat.server.db :as db]))

(defn update-for-user
  [user-id]
  (db/with-conn
    (db/get-open-threads-for-user user-id)))

; Scheduling

(defjob SendEmailsJob
  [ctx]
  (timbre/debugf "Starting email job")
  )

(defn email-job
  []
  (j/build
    (j/of-type SendEmailsJob)
    (j/with-identity (j/key "jobs.email-send.1"))))

(defn email-trigger
  []
  (t/build
    (t/with-identity (t/key "triggers.email-send"))
    (t/start-now)
    (t/with-schedule
      (cron/schedule
        (cron/daily-at-hour-and-minute 3 0)))))
