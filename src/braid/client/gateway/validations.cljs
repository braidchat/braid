(ns braid.client.gateway.validations
  (:require
    [clojure.string :as string]
    [braid.client.xhr :as xhr])
  (:import
    [goog.format EmailAddress]))

(def validations
  {:gateway.user-auth/email
   [(fn [email cb]
      (if (string/blank? email)
        (cb "You need to enter an email.")
        (cb nil)))
    (fn [email cb]
      (if (not (.isValid (EmailAddress. email)))
        (cb "This doesn't look like a valid email.")
        (cb nil)))]

   :gateway.user-auth/password
   [(fn [password cb]
      (if (string/blank? password)
        (cb "You need to enter a password.")
        (cb nil)))]

   :gateway.user-auth/new-password
   [(fn [password cb]
      (if (string/blank? password)
        (cb "You need to enter a password.")
        (cb nil)))
    (fn [password cb]
      (if (< (count password) 8)
        (cb "Your password is too short.")
        (cb nil)))]

   :gateway.action.create-group/group-name
   [(fn [name cb]
      (if (string/blank? name)
        (cb "Your group needs a name.")
        (cb nil)))]

   :gateway.action.create-group/group-url
   [(fn [url cb]
      (if (string/blank? url)
        (cb "Your group needs a URL.")
        (cb nil)))
    (fn [url cb]
      (if (not (re-matches #"[a-z0-9-]*" url))
        (cb "Your URL can only contain lowercase letters, numbers or dashes.")
        (cb nil)))
    (fn [url cb]
      (if (re-matches #"-.*" url)
        (cb "Your URL can't start with a dash.")
        (cb nil)))
    (fn [url cb]
      (if (re-matches #".*-" url)
        (cb "Your URL can't end with a dash.")
        (cb nil)))
    (fn [url cb]
      (xhr/edn-xhr
        {:uri "/registration/check-slug-unique"
         :method :get
         :params {:slug url}
         :on-complete (fn [valid?]
                        (if valid?
                          (cb nil)
                          (cb "Your group URL is already taken; try another.")))
         :on-error (fn [_]
                     (cb "There was an error checking your URL."))}))]

   :gateway.action.create-group/group-type
   [(fn [type cb]
      (if (string/blank? type)
        (cb "You need to select a group type.")
        (cb nil)))]})
