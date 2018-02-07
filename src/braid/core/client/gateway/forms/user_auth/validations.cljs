(ns braid.core.client.gateway.forms.user-auth.validations
  (:require
   [clojure.string :as string])
  (:import
   (goog.format EmailAddress)))

(def validations
  {:email
   [(fn [email cb]
      (if (string/blank? email)
        (cb "You need to enter an email.")
        (cb nil)))
    (fn [email cb]
      (if (not (.isValid (EmailAddress. email)))
        (cb "This doesn't look like a valid email.")
        (cb nil)))]

   :password
   [(fn [password cb]
      (if (string/blank? password)
        (cb "You need to enter a password.")
        (cb nil)))]

   :new-password
   [(fn [password cb]
      (if (string/blank? password)
        (cb "You need to enter a password.")
        (cb nil)))
    (fn [password cb]
      (if (< (count password) 8)
        (cb "Your password is too short.")
        (cb nil)))]})
