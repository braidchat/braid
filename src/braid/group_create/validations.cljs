(ns braid.group-create.validations
  (:require
   [clojure.string :as string]
   [braid.lib.xhr :as xhr]))

(def validations
  {:group-name
   [(fn [name cb]
      (if (string/blank? name)
        (cb "Your group needs a name.")
        (cb nil)))]

   :group-url
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

   :group-type
   [(fn [type cb]
      (if (string/blank? type)
        (cb "You need to select a group type.")
        (cb nil)))]})
