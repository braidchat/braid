(ns braid.lib.xhr
  (:require
    [ajax.core :refer [ajax-request json-request-format json-response-format]]
    [ajax.edn :refer [edn-request-format edn-response-format]]
    [goog.object :as o]))

(defn edn-xhr
  [args]
  (ajax-request (assoc args
                  :uri (str "/api" (args :uri))
                  :with-credentials true
                  :format (edn-request-format)
                  :response-format (edn-response-format)
                  :handler (let [on-error-fn (or (args :on-error) identity)
                                 on-complete-fn (or (args :on-complete) identity)]
                             (fn [[ok? data]]
                               (if ok?
                                 (on-complete-fn data)
                                 (on-error-fn data)))))))

(defn ajax-xhr
  [args]
  (ajax-request (assoc args
                  :format (json-request-format)
                  :response-format (json-response-format {:keywords? true})
                  :handler (let [on-error-fn (or (args :on-error) identity)
                                 on-complete-fn (or (args :on-complete) identity)]
                             (fn [[ok? data]]
                               (if ok?
                                 (on-complete-fn data)
                                 (on-error-fn data)))))))
