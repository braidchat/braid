(ns braid.client.register.validations
  (:require
    [clojure.string :as string]
    [ajax.core :refer [ajax-request]]
    [ajax.edn :refer [edn-request-format edn-response-format]])
  (:import
    [goog.format EmailAddress]))

(def validations
  {:email [(fn [email cb]
             (if (string/blank? email)
               (cb "You need to enter your email.")
               (cb nil)))
           (fn [email cb]
             (if (not (.isValid (EmailAddress. email)))
               (cb "This doesn't look like a valid email.")
               (cb nil)))]
   :name [(fn [name cb]
            (if (string/blank? name)
              (cb "Your group needs a name.")
              (cb nil)))]
   :url [(fn [url cb]
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
           (ajax-request
             {:uri (str "//" js/window.api_domain "/registration/check-slug-unique")
              :method :get
              :format (edn-request-format)
              :response-format (edn-response-format)
              :params {:slug url}
              :handler (fn [[_ valid?]]
                         (if valid?
                           (cb nil)
                           (cb "Your group URL is already taken; try another.")))}))]
   :type [(fn [type cb]
            (when (string/blank? type)
              (cb "You need to select a group type.")
              (cb nil)))]})
