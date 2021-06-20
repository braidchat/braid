(ns braid.test.lib.xpath
  (:require
   [clojure.string :as string]
   [instaparse.core :as insta]))

(def css-selector-parser
  (insta/parser
    "S ::= ( CLASS-SELECTOR | COMBINATOR ) *

     CLASS-SELECTOR ::= '.' CLASS
     CLASS ::= #'[a-zA-Z-]+'
     COMBINATOR ::= DESCENDANT-COMB | DIRECT-CHILD-COMB
     DESCENDANT-COMB ::= ' '+
     DIRECT-CHILD-COMB ::= '>' | ' > '

    "))

(defn css->xpath
  [css-selector]
  (->> (css-selector-parser css-selector)
       (insta/transform {:CLASS-SELECTOR (fn [_ [_ class-name]]
                                           class-name)
                         :COMBINATOR (fn [[type]]
                                       (case type
                                         :DIRECT-CHILD-COMB "/"
                                         :DESCENDANT-COMB "//"))})
       rest
       (map (fn [arg]
              (cond
                (#{"/" "//"} arg)
                arg

                (string? arg)
                (str "*[contains(concat(' ',normalize-space(@class),' '),' "
                     arg
                     " ')]"))))
       string/join
       (str "//")))

(comment
  (prn (css->xpath ".foo .bar")))
