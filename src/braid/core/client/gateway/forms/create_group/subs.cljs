(ns braid.core.client.gateway.forms.create-group.subs
  (:require
   [braid.core.client.gateway.helpers :as helpers]
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::sending?
  (fn [state _]
    (get-in state [:create-group :sending?])))

(reg-sub
  ::error
  (fn [state _]
    (get-in state [:create-group :error])))

(helpers/reg-form-subs :braid.core.client.gateway.forms.create-group.subs :create-group)
