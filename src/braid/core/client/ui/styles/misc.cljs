(ns braid.core.client.ui.styles.misc
  (:require
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.arithmetic :as m]
   [garden.units :refer [rem em px ex]]))

(def page-headers
  [[:.page>.title::before
    {:margin-right (em 0.5)}]

   [:.page.users>.title::before
    (mixins/fontawesome \uf0c0)]

   [:.page.settings>.title::before
    (mixins/fontawesome \uf013)]

   [:.page.search>.title::before
    (mixins/fontawesome \uf002)]

   [:.page.me>.title::before
    (mixins/fontawesome \uf007)]

   [:.page.userpage>.title::before
    (mixins/fontawesome \uf007)]])


(defn threads [pad]
  [:>.threads
   mixins/flex
   {:position "absolute"
    :top "2.6rem"
    :right 0
    :bottom 0
    :left 0
    :padding-left vars/pad
    :align-items "flex-end"
    :overflow-x "auto" }])

(def status
  [:>.status
   mixins/flex
   {:position "absolute"
    :top 0
    :bottom 0
    :right 0
    :left 0
    :background vars/page-background-color
    :color vars/primary-text-color
    :justify-content "center"
    :align-items "center"
    :font-size "2em"}

   [:&::before
    (mixins/fontawesome \uf110)
    mixins/spin
    {:display "inline-block"
     :margin-right (em 0.5)}]])

(defn drag-and-drop [pad]
  [:&

   [:&.dragging
    {:background-color "lightgray"
     :border [[(px 5) "dashed" "darkgray"]]}]

   [:>.uploading-indicator
    (mixins/fontawesome \uf110)
    mixins/spin
    {:font-size (em 1.5)
     :text-align "center"}]])

(defn avatar-upload [pad]
  [:&
   (drag-and-drop pad)])
