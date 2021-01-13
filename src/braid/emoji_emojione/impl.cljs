(ns braid.emoji-emojione.impl
  (:require
    [garden.units :refer [rem em px ex]]
    [braid.emoji-emojione.ref :as refs]))

;; Taken from https://github.com/emojione/emojione/blob/2.2.7/assets/css/emojione.css#L1
(def emojione-styles
  [:.emojione
   {:font-size "inherit"
    :height (ex 3)
    :width (ex 3.1)
    :min-height (px 20)
    :min-width (px 20)
    :display "inline-block"
    :margin [[(ex -0.2) (em 0.15) (ex 0.2)]]
    :line-height "normal"
    :vertical-align "middle"}
   ;; Taken from https://github.com/emojione/emojione/blob/2.2.7/assets/css/emojione-awesome.css#L24
   [:&.large
    {:width (em 3)
     :height (em 3)
     :margin [[(ex -0.6) (em 0.15) 0 (em 0.3)]]
     :background-size [[(em 3) (em 3)]]}]])

(defn- emoji-meta [url]
  {:class "emojione"
   :src url})

(defn- emojione-url [id]
  (str "//cdn.jsdelivr.net/emojione/assets/png/" id ".png"))

(defn shortcode-lookup []
  (->> refs/shortcodes
       (reduce (fn [memo [shortcode ids]]
                 (assoc memo shortcode
                   (emoji-meta (emojione-url (last ids))))) {})))

(defn ascii-lookup []
  (->> refs/ascii
       (reduce (fn [memo [ascii shortcode]]
                 (let [id (last (refs/shortcodes shortcode))]
                   (assoc memo ascii
                     (emoji-meta (emojione-url id))))) {})))
