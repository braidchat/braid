(ns braid.client.ui.styles.imports
  (:require
    [garden.stylesheet :refer [at-import at-font-face]]))

(def fa-at-font-face
  (let [version "4.7.0"
        fa-cdn-url (str "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/" version)]
    (at-font-face
      {:font-family "FontAwesome"
       :src [(str "local('FontAwesome')")
             (str "url('" fa-cdn-url "/fonts/fontawesome-webfont.eot?#iefix&v=" version "') format('embedded-opentype')")
             (str "url('" fa-cdn-url "/fonts/fontawesome-webfont.woff2?v=" version "') format('woff2')")
             (str "url('" fa-cdn-url "/fonts/fontawesome-webfont.woff?v=" version "') format('woff')")
             (str "url('" fa-cdn-url "/fonts/fontawesome-webfont.ttf?v=" version "') format('truetype')")
             (str "url('" fa-cdn-url "/fonts/fontawesome-webfont.svg?v=" version "#fontawesomeregular') format('svg')")]
       :font-weight "normal"
       :font-style "normal"})))

(def imports
  [fa-at-font-face
   (at-import "https://fonts.googleapis.com/css?family=Open+Sans:400,300,400italic,700")])
