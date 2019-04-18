(ns braid.bots.schema
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]))

(def Bot
  {:id uuid?
   :group-id uuid?
   :user-id uuid?
   :name string?
   :avatar string?
   :webhook-url string?
   :event-webhook-url (ds/maybe string?)
   :token string?
   :notify-all-messages? boolean?})

(def BotDisplay
  "Like Bot but for publicly-available bot info"
  {:id uuid?
   :user-id uuid?
   :nickname string?
   :avatar string?})
