(ns braid.core.client.ui.styles.hljs)

(def foreground "#eaeaea")
(def background "#000000")
(def selection "#424242")
(def line "#2a2a2a")
(def comment-color "#969896")
(def red "#d54e53")
(def orange "#e78c45")
(def yellow "#e7c547")
(def green "#b9ca4a")
(def aqua "#70c0b1")
(def blue "#7aa6da")
(def purple "#c397d8")
(def window "#4d5057")

(def hljs-styles
  [:>.hljs

   [:.hljs-comment
    :.hljs-quote
    {:color comment-color}]

   [:.hljs-variable
    :.hljs-template-variable
    :.hljs-tag
    :.hljs-name
    :.hljs-selector-id
    :.hljs-selector-class
    :.hljs-regexp
    :.hljs-deletion
    {:color red}]

   [:.hljs-number
    :.hljs-built_in
    :.hljs-builtin-name
    :.hljs-literal
    :.hljs-type
    :.hljs-params
    :.hljs-meta
    :.hljs-link
    {:color orange}]

   [:.hljs-attribute
    {:color yellow}]

   [:.hljs-symbol
    {:color aqua}]

   [:.hljs-string
    :.hljs-bullet
    :.hljs-addition
    {:color green}]

   [:.hljs-title
    :.hljs-section
    {:color blue}]

   [:.hljs-keyword
    :.hljs-selector-tag
    {:color purple}]

   [:.hljs-emphasis
    {:font-style "italic"}]

   [:.hljs-strong
    {:font-weight "bold"}]])
