(ns braid.server.markdown
  (:require [instaparse.core :as insta]
            [clojure.string :as string]))

; TODO: refactor this grammar to remove ambiguities - currently issues where
; PLAIN_TEXT is ambiguous, since you can have [:PLAIN_TEXT "foo"] or
; [:PLAIN_TEXT "f"] [:PLAIN_TEXT "oo"], etc
(def markdown-parser
  "Simple markdown parser. Only parsing enough to handle CHANGELOG.md, so lots
  is probably missing"
  (insta/parser
    "S ::= ( ( HEADER | LIST | <BLANK_LINE> ) / PARAGRAPH ) *

    <STARTL> ::= #'(?m)\\A|^'
    <ENDL> ::= #'\\n|\\z'
    ws ::= #'[ \\t\\x0b]*'

    <DOT> ::= #'.'
    PLAIN_TEXT ::= ( !LINK DOT ) +
    URL ::= #'\\S' +
    LINK ::= <'['> PLAIN_TEXT <']('> URL <')'>
    <TEXT> ::=  ( LINK / PLAIN_TEXT ) ( TEXT ?)

    HEADER ::= <STARTL> #'#+' <ws> TEXT <ws> <'#'*> <ENDL>

    LIST ::= ( <STARTL> LIST_LINE <ENDL> ) +
    LIST_LINE ::= <#'\\s+(-|\\*)'> <ws> TEXT

    PARAGRAPH ::= PLAIN_LINE+ <BLANK_LINE*>
    BLANK_LINE ::= STARTL ws ENDL
    <PLAIN_LINE> ::= <STARTL> !BLANK_LINE TEXT <ENDL>
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
                         :PARAGRAPH (fn [& lines]
                                      (vec (cons :p lines)))
                         :S (fn [& args]
                              (vec (cons :div args)))})))
