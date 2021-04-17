(ns braid.core.client.ui.styles.imports
  (:require
   [garden.stylesheet :refer [at-import at-font-face]]))

(def fa-at-font-face
  (at-font-face
    {:font-family "\"Font Awesome 5 Free\""
     :src ["url('/fonts/fa-solid-900.eot?#iefix') format('embedded-opentype')"
           "url('/fonts/fa-solid-900.woff2') format('woff2')"
           "url('/fonts/fa-solid-900.woff') format('woff')"
           "url('/fonts/fa-solid-900.ttf') format('truetype')"
           "url('/fonts/fa-solid-900.svg#fontawesome') format('svg')"]
     :font-weight 900
     :font-display "block"
     :font-style "normal"}))

(def fab-at-font-face
  (at-font-face
    {:font-family "\"Font Awesome 5 Brands\""
     :src ["url('/fonts/fa-brands-400.eot?#iefix') format('embedded-opentype')"
           "url('/fonts/fa-brands-400.woff2') format('woff2')"
           "url('/fonts/fa-brands-400.woff') format('woff')"
           "url('/fonts/fa-brands-400.ttf') format('truetype')"
           "url('/fonts/fa-brands-400.svg#fontawesome') format('svg')"]
     :font-weight 400
     :font-display "block"
     :font-style "normal"}))

(def imports
  [fa-at-font-face
   fab-at-font-face
   (at-import "https://fonts.googleapis.com/css?family=Open+Sans:400,300,400italic,700")])
