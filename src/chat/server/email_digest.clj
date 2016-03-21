(ns chat.server.email-digest
  (:require [clojurewerkz.quartzite
             [triggers :as t]
             [jobs :as j :refer [defjob]]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [clj-time
             [core :as time]
             [coerce :refer [to-date-time]]]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [chat.server.db :as db]))

; finding data

(defn thread-unseen?
  "A thread is unseen if it's last-open-at is prior to any of the messages' created-at"
  [thread]
  (let [last-open (to-date-time (thread :last-open-at))]
    (some (fn [{:keys [created-at]}]
            (time/before? last-open (to-date-time created-at)))
          (thread :messages))))

(defn updates-for-user
  [user-id]
  (db/with-conn
    (doall (filter thread-unseen? (db/get-open-threads-for-user user-id)))))

(defn daily-update-users
  "Find all users that want daily digest updates"
  [])

; sending

(defn send-message
  [to {:keys [text html]}]
  (http/post (str "https://api.mailgun.net/v3/" (env :mailgun-domain) "/messages")
             {:basic-auth ["api" (env :mailgun-password)]
              :form-params {:to to
                            :from (str "noreply@" (env :mailgun-domain))
                            :subject "While you were away"
                            :text text
                            :html html}}))

;; Scheduling

; daily digest
(defjob DailyDigestJob
  [ctx]
  (timbre/debugf "Starting daily email job"))

(defn daily-digest-job
  []
  (j/build
    (j/of-type DailyDigestJob)
    (j/with-identity (j/key "jobs.daily-email.1"))))

(defn daily-digest-trigger
  []
  (t/build
    (t/with-identity (t/key "triggers.daily-email"))
    (t/start-now)
    (t/with-schedule
      (cron/schedule
        (cron/daily-at-hour-and-minute 3 0)))))

; weekly digest
(defjob WeeklyDigestJob
  [ctx]
  (timbre/debugf "Starting weekly email job"))

(defn weekly-digest-job
  []
  (j/build
    (j/of-type WeeklyDigestJob)
    (j/with-identity (j/key "jobs.weekly-email.1"))))

(defn weekly-digest-trigger
  []
  (t/build
    (t/with-identity (t/key "triggers.weekly-email"))
    (t/start-now)
    (t/with-schedule
      (cron/schedule
        (cron/weekly-on-day-and-hour-and-minute 0 2 30)))))
