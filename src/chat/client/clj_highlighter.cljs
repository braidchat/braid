(ns chat.client.clj-highlighter)

(defn install-highlighter
  []
  (let [PR (.-PR js/window)]
    (PR.registerLangHandler
      (PR.createSimpleLexer
        (clj->js
          [
           ["opn"  #"^[\(\{\[]+" nil "([{"]
           ["clo"  #"^[\)\}\]]+" nil ")]}"]
           [(.-PR_COMMENT PR) #"^;[^\r\n]*" nil ";"]
           [(.-PR_PLAIN PR) #"^[\t\n\r \u00A0]+" nil "\t\n\r \u00A0"]
           [(.-PR_STRING PR) #"^\"(?:[^\"\\]|\\[\s\S])*(?:\"|$)" nil "\""]
           ])
        (clj->js
          [
           [(.-PR_KEYWORD PR) #"\b(?:def|if|do|let|quote|var|fn|loop|recur|throw|try|monitor-enter|monitor-exit|defmacro|defn|defn-|macroexpand|macroexpand-1|for|doseq|dosync|dotimes|and|or|when|not|assert|doto|proxy|defstruct|first|rest|cons|defprotocol|deftype|defrecord|reify|defmulti|defmethod|meta|with-meta|ns|in-ns|create-ns|import|intern|refer|alias|namespace|resolve|ref|deref|refset|new|set!|memfn|to-array|into-array|aset|gen-class|reduce|map|filter|find|nil\?|empty\?|hash-map|hash-set|vec|vector|seq|flatten|reverse|assoc|dissoc|list|list\?|disj|get|union|difference|intersection|extend|extend-type|extend-protocol|prn)(?:\s|$)"]
           [(.-PR_TYPE PR) #":[0-9a-zA-Z\-]+"]]) )
      #js ["clj"])))
