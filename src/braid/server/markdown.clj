(ns braid.server.markdown
  (:require [instaparse.core :as insta]
            [clojure.string :as string]))

(def markdown-parser
  "Simple markdown parser. Only parsing enough to handle CHANGELOG.md, so lots
  is probably missing"
  (insta/parser
    "S ::= ( <#'^'> LINE <ENDL> )*
    ENDL ::= #'\\n|$'
    <LINE> ::= ( HEADER | LIST ) / PLAIN_LINE

    ws ::= #'[ \\t\\x0b]*'

    <DOT> ::= #'.'
    PLAIN_TEXT ::= ( !LINK DOT ) +
    URL ::= #'\\S' +
    LINK ::= <'['> PLAIN_TEXT <']('> URL <')'>
    <TEXT> ::=  ( LINK / PLAIN_TEXT ) ( TEXT ?)

    HEADER ::= #'#+' <ws> TEXT <ws> <'#'*>

    LIST ::= ( LIST_LINE <ENDL> ) +
    LIST_LINE ::= <#'\\s+(-|\\*)'> <ws> TEXT

    PLAIN_LINE ::= TEXT
    "))

(defn markdown->hiccup
  "Parse markdown into hiccup."
  [md-str]
  (->> (markdown-parser md-str)
       (insta/transform {:HEADER (fn [delim & rst]
                                   (vec (cons (keyword (str "h" (count delim)))
                                              rst)))
                         :PLAIN_TEXT (fn [& args] (string/join "" args))
                         :URL (fn [& args] (string/join "" args))
                         :LINK (fn [title url]
                                 [:a {:href url} title])
                         :LIST (fn [& args]
                                 (vec (cons :ul args)))
                         :LIST_LINE (fn [& args]
                                      (vec (cons :li args)))
                         :PLAIN_LINE (fn [& args]
                                       (vec (cons :span args)))
                         :S (fn [& args]
                              (vec (cons :div args)))})))
