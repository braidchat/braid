(ns braid.core.client.ui.styles.fontawesome
  (:require
    [braid.core.client.ui.styles.mixins :as mixins]))

(def icons
 {:github \uf09b
  :facebook \uf09a
  :google \uf1a0
  :caret-right \uf0da
  :chevron-right \uf054
  :angle-right \uf105
  :angle-double-right \uf101
  :warning \uf071
  :check-circle \uf00c
  :spinner \uf110
  :users \uf0c0})

(defn mixin [code]
  (mixins/fontawesome (or (icons code) code)))
