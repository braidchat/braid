(ns braid.client.ui.styles.fontawesome)

(def icons
 {:github \uf09b
  :facebook \uf09a
  :google \uf1a0})

(defn mixin [code]
  {:font-family "fontawesome"
   :content  (str "\"" (or (icons code) code) "\"")})
