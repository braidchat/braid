(ns braid.lib.color
  (:require
    [clojure.string :as string]
    [cljsjs.husl]
    [braid.lib.url :as url]))

(defn ->color [input]
  (js/window.HUSL.toHex (mod (Math/abs (hash input)) 360) 95 50))

(defn url->color [url]
  (-> url
      string/lower-case
      url/url->parts
      :domain
      ->color))
