(ns chat.server.email-digest
  (:require [clojurewerkz.quartzite
             [triggers :as t]
             [jobs :as j :refer [defjob]]
             [scheduler :as qs]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [clj-time
             [core :as time]
             [coerce :refer [to-date-time]]]
            [environ.core :refer [env]]
            [clostache.parser :refer [render-resource]]
            [inliner.core :refer [inline-css]]
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

(defn last-message-after?
  [cutoff thread]
  (some (fn [{:keys [created-at]}]
          (time/after? (to-date-time created-at) cutoff))
        (thread :messages)))

(defn updates-for-user-since
  [user-id cutoff]
  (db/with-conn
    (let [users (db/fetch-users-for-user user-id)
          id->nick (into {} (map (juxt :id :nickname)) users)
          id->avatar (into {} (map (juxt :id :avatar)) users)]
      (into ()
            (comp
              (filter thread-unseen?)
              (filter (partial last-message-after? cutoff))
              (map
                (fn [t]
                  (update t :messages
                          (partial map
                                   (fn [{sender-id :user-id :as m}]
                                     (-> m
                                         (assoc :sender (id->nick sender-id))
                                         (assoc :sender-avatar (id->avatar sender-id)))))))))
            (db/get-open-threads-for-user user-id)))))

(defn daily-update-users
  "Find all ids for users that want daily digest updates"
  []
  (db/with-conn (db/user-search-preferences :email-frequency :daily)))

(defn weekly-update-users
  "Find all ids for users that want weekly digest updates"
  []
  (db/with-conn (db/user-search-preferences :email-frequency :weekly)))

; build a message from a thread

(defn create-message
  [threads]
  {:html  (inline-css (render-resource "templates/email_digest.html.mustache"
                                       {:threads threads}))
   :text (render-resource "templates/email_digest.txt.mustache" {:threads threads})})

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
  (timbre/debugf "Starting daily email job")
  (let [user-ids (daily-update-users)
        cutoff (time/minus (time/now) (time/days 1))]
    (doseq [uid user-ids]
      (when-let [threads (seq (updates-for-user-since uid cutoff))]
        (let [email (db/with-conn (db/user-email uid))]
          (send-message email (create-message threads)))))))

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
  (timbre/debugf "Starting weekly email job")
  (let [user-ids (daily-update-users)
        cutoff (time/minus (time/now) (time/days 7))]
    (doseq [uid user-ids]
      (when-let [threads (seq (updates-for-user-since uid cutoff))]
        (let [email (db/with-conn (db/user-email uid))]
          (send-message email (create-message threads)))))))

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
        (cron/weekly-on-day-and-hour-and-minute 1 2 30)))))

(defn add-jobs
  [scheduler]
  (qs/schedule scheduler (daily-digest-job) (daily-digest-trigger))
  (qs/schedule scheduler (weekly-digest-job) (weekly-digest-trigger)))
