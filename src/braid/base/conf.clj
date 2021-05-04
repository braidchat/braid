(ns braid.base.conf
  (:require
   [braid.base.conf-extra :refer [ports-config]]
   [braid.core.hooks :as hooks]
   [malli.core :as malli]
   [malli.error :as malli.error]
   [malli.transform :as malli.transform]
   [clojure.pprint :as pprint]
   [mount.core :as mount :refer [defstate]]))

(defonce config-vars
  (hooks/register! (atom []) [any?]))

(defn ->malli-schema
  [config-vars]
  (->> config-vars
       (map (fn [{:keys [key required? schema]}]
              [key {:optional (not required?)} schema]))
       (into [:map])))

(defn select-and-validate
  "Extracts and validates keys from a given env map according to config-vars.
     Removes keys that aren't defined in schema.
     Throws an error when missing keys or values are invalid."
  [env config-vars]
  (let [schema (->malli-schema config-vars)
        env (malli/decode schema env (malli.transform/transformer
                                       malli.transform/strip-extra-keys-transformer
                                       malli.transform/string-transformer))]
    (if (malli/validate schema env)
      env
      (let [cause (malli/explain schema env)]
        (throw
          (ex-info (str "Config invalid\n"
                        (with-out-str
                          (pprint/pprint (malli.error/humanize cause)))) cause))))))

#_(select-and-validate
   {:foo "123"
    :bar 2}
   [{:key :foo
     :required? false
     :schema [:int]}])


(defstate config
  :start
  (merge ;; temp defaults
    ;; TODO don't special case these here
    ;; ports should come from config
    {:db-url "datomic:mem://braid"
     :site-url (str "http://localhost:" (:port (mount/args)))
     :hmac-secret "secret"
     :app-title "Braid"}
    @ports-config ; overrides site url when port is automatically found
    (select-and-validate (mount/args) @config-vars)))
