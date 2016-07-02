(ns braid.client.ui.styles.imports
  (:require [garden.stylesheet :refer [at-import]]))

(def imports
  [(at-import "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css")
   (at-import "https://fonts.googleapis.com/css?family=Open+Sans:400,300,400italic,700")])
