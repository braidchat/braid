(ns braid.client.router
  (:require [secretary.core :as secretary]
            [goog.history.EventType :as EventType]
            [goog.events :as events])
  (:import [goog.history Html5History]
           [goog Uri]))

(defn init []
  (let [h (Html5History.)]
    (doto h
      (.setUseFragment false)
      (.setPathPrefix "")
      (.setEnabled true))
    (events/listen h EventType/NAVIGATE (fn [e] (secretary/dispatch! (.-token e))))

    (defn go-to [path]
      (. h (setToken path)))

    (events/listen js/document.body "click"
      (fn [e]
        ; if this element has href, or any parent has href, then pushstate
        (when-let [href (loop [e (.-target e)]
                          (if-let [href (.-href e)]
                            href
                            (when-let [parent (.-parentNode e)]
                              (recur parent))))]
          (when (.hasSameDomainAs (.parse Uri href) (.parse Uri js/window.location))
            (let [path (.getPath (.parse Uri href))]
              (when (secretary/locate-route path)
                (. h (setToken path))
                (.preventDefault e)))))))))

(defn dispatch-current-path! []
  (secretary/dispatch! (.getPath (.parse Uri js/window.location))))
