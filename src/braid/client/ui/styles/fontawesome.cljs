(ns braid.client.ui.styles.fontawesome)

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
  :spinner \uf110})

(defn mixin [code]
  {:font-family "fontawesome"
   :content  (str "\"" (or (icons code) code) "\"")})
