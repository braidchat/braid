(ns chat.client.views.message
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.emoji :as emoji]
            [chat.client.views.helpers :as helpers]
            [chat.client.views.pills :refer [tag-view user-view]]
            ))

(def replacements
  {:urls
   {:pattern #"(http(?:s)?://\S+(?:\w|\d|/))"
    :replace (fn [match]
               (dom/a #js {:href match :target "_blank"} match))}
   :users
   {:pattern #"@([-0-9a-z]+)"
    :replace (fn [match]
               (if (store/valid-user-id? (uuid match))
                 (om/build user-view {:id (uuid match)})
                 (dom/span nil "@" match)))}
   :tags
   {:pattern #"#([-0-9a-z]+)"
    :replace (fn [match]
               (if-let [tag (store/get-tag (uuid match))]
                 (om/build tag-view tag)
                 (dom/span nil "#" match)))}
   :emoji-shortcodes
   {:pattern #"(:\S*:)"
    :replace (fn [match]
               (if (emoji/unicode match)
                 (emoji/shortcode->html match)
                 match))}
   :emoji-ascii
   {:replace (fn [match]
               (if-let [shortcode (emoji/ascii match)]
                 (emoji/shortcode->html shortcode)
                 match))}
   })

(defn re-replace
  [re s replace-fn]
  (if-let [match (second (re-find re s))]
    ; TODO: recurse, incease the rest has more matches?
    ; using Javascript split beacuse we don't want the match to be in the last
    ; component
    (let [[pre _ post] (seq (.split s re 3))]
      (if (or (string/blank? pre) (re-matches #".*\s$" pre))
      ; XXX: find a way to use return a seq & use mapcat instead of this hack
      (dom/span #js {:className "dummy"} pre (replace-fn match) post)
      s))
    s))

(defn make-text-replacer
  "Make a new function to perform a simple stateless replacement of a single element"
  [match-type]
  (fn [text-or-node]
    (if (string? text-or-node)
      (let [text text-or-node
            type-info (get replacements match-type)]
        (re-replace (type-info :pattern) text (type-info :replace)))
      text-or-node)))

; TODO: clojure 1.8 should implement this
(defn starts-with? [s prefix]
  ; not using .startsWith because it's only supported in ES6
  (= 0 (.indexOf s prefix)))

(defn make-delimited-processor
  "Make a new transducer to process the stream of words"
  [{:keys [delimiter result-fn]}]
  (fn [xf]
    (let [state (volatile! ::start)
          in-code (volatile! [])]
      (fn
        ([] (xf))
        ([result] (if (= @state ::in-code)
                    (reduce xf result (update-in @in-code [0] (partial str delimiter)))
                    (xf result)))
        ([result input]
         (if (string? input)
           (cond
             ; TODO: handle starting code block with delimiter not at beginning of word
             ; start
             (and (= @state ::start) (starts-with? input delimiter))
             (cond
               (and (not= input delimiter) (.endsWith input delimiter))
               (xf result (result-fn (.slice input (count delimiter) (- (.-length input) (count delimiter)))))

               (and (not= input delimiter) (not= 0 (.lastIndexOf input delimiter)))
               (let [idx (.lastIndexOf input delimiter)
                     code (.slice input (count delimiter) idx)
                     after (.slice input (inc idx) (.-length input))]
                 (reduce xf result [(result-fn code) after]))

               :else
               (do (vreset! state ::in-code)
                   (vswap! in-code conj (.slice input (count delimiter)))
                   result))

             ; end
             (and (= @state ::in-code) (.endsWith input delimiter))
             (let [code (conj @in-code (.slice input 0 (- (.-length input) (count delimiter))))]
               (vreset! state ::start)
               (vreset! in-code [])
               (xf result (result-fn (string/join " " code))))

             (and (= @state ::in-code) (not= -1 (.indexOf input delimiter)))
             (let [idx (.indexOf input delimiter)
                   code (conj @in-code (.slice input 0 idx))
                   after (.slice input (inc idx) (.-length input))]
               (vreset! state ::start)
               (vreset! in-code [])
               (reduce xf result [(result-fn (string/join " " code)) after]))

             (= @state ::in-code) (do (vswap! in-code conj input) result)

             :else (xf result input))
           (xf result input)))))))

(def url-replace (make-text-replacer :urls))
(def user-replace (make-text-replacer :users))
(def tag-replace (make-text-replacer :tags))
(def emoji-shortcodes-replace (make-text-replacer :emoji-shortcodes))

(defn emoji-ascii-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node]
      (if (contains? emoji/ascii-set text)
        ((get-in replacements [:emoji-ascii :replace]) text)
        text))
    text-or-node))

(def extract-code-blocks
  (make-delimited-processor {:delimiter "```"
                             :result-fn (partial dom/code #js {:className "prettyprint multiline-code lang-clj"})}))
(def extract-code-inline
  (make-delimited-processor {:delimiter "`"
                             :result-fn (partial dom/code #js {:className "prettyprint inline-code lang-clj"})}))

(def extract-emphasized
  (make-delimited-processor {:delimiter "*"
                             :result-fn (partial dom/strong #js {:className "starred"})}))



(defn format-message
  "Given the text of a message body, turn it into dom nodes, making urls into
  links"
  [text]
  (let [; Caution: order of transforms is important! url-replace should come before
        ; user/tag replace at least so urls with octothorpes or at-signs don't get
        ; wrecked
        stateless-transform (map (comp emoji-ascii-replace
                                       emoji-shortcodes-replace
                                       tag-replace
                                       user-replace
                                       url-replace))
        statefull-transform (comp extract-code-blocks extract-code-inline extract-emphasized)]
    (->> (into [] (comp statefull-transform stateless-transform) (string/split text #" "))
         (interleave (repeat " "))
         rest)))

(defn message-view [message owner opts]
  (reify
    om/IDidMount
    (did-mount [_]
      ; TODO: use prettyPrintOne to only do the content of this node
      ; TODO: also call on IDidUpdate?
      ; TODO: don't call if don't have code?
      (when-let [PR (aget js/window "PR")]
        ((aget PR "prettyPrint"))))
    om/IRender
    (render [_]
      (let [sender (om/observe owner (helpers/user-cursor (message :user-id)))]
        (dom/div #js {:className (str "message"
                                      " " (when (:collapse? opts) "collapse")
                                      " " (if (:unseen? message) "unseen" "seen")
                                      " " (when (:first-unseen? message) "first-unseen"))}
          (dom/img #js {:className "avatar"
                        :src (sender :avatar)
                        :style #js {:backgroundColor (id->color (sender :id))}
                        :onClick (fn [_] (store/set-page! {:type :user :id (sender :id)}))})
          (dom/div #js {:className "info"}
            (dom/span #js {:className "nickname"
                           :onClick (fn [_] (store/set-page! {:type :user :id (sender :id)}))}
              (sender :nickname))
            (dom/span #js {:className "time"} (helpers/format-date (message :created-at))))
          (apply dom/div #js {:className "content"}
            (format-message (message :content))))))))
