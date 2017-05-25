(ns braid.client.gateway.forms.create-group.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [braid.client.gateway.helpers :as helpers]))

(reg-sub
  ::sending?
  (fn [state _]
    (get-in state [:create-group :sending?])))

(reg-sub
  ::error
  (fn [state _]
    (get-in state [:create-group :error])))

(helpers/reg-form-subs :braid.client.gateway.forms.create-group.subs :create-group)
