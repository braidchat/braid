(ns chat.client.views.helpers
  (:require [om.dom :as dom]
            [clojure.string :as string]
            [cljs-utils.core :refer [flip]]
            [cljs-time.format :as f]
            [cljs-time.core :as t]
            [chat.client.emoji :as emoji]))

(defn tag->color [tag]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> (tag :id)
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",70%,35%)")))


(def replacements
  {:urls
   {:pattern #"(http(?:s)?://\S+(?:\w|\d|/))"
    :replace (fn [match]
               (dom/a #js {:href match :target "_blank"} match))}
   :users
   {:pattern #"@(\S*)"
    :replace (fn [match]
               (dom/span #js {:className "user-mention"} "@" match)) }
   :tags
   {:pattern #"#(\S*)"
    :replace (fn [match]
               (dom/span #js {:className "tag-mention"} "#" match))}
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

(defn text-replacer
  [match-type]
  (fn [text-or-node]
    (if (string? text-or-node)
      (let [text text-or-node
            pattern (get-in replacements [match-type :pattern])]
        (if-let [match (second (re-find pattern text))]
          ((get-in replacements [match-type :replace]) match)
          text))
      text-or-node)))

(def url-replace (text-replacer :urls))
(def user-replace (text-replacer :users))
(def tag-replace (text-replacer :tags))
(def emoji-shortcodes-replace (text-replacer :emoji-shortcodes))

(defn emoji-ascii-replace [text-or-node]
  (if (string? text-or-node)
    (let [text text-or-node]
      (if (contains? emoji/ascii-set text)
        ((get-in replacements [:emoji-ascii :replace]) text)
        text))
    text-or-node))

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
             (and (= @state ::start) (.startsWith input delimiter))
             (if (and (not= input delimiter) (.endsWith input delimiter))
               (xf result (result-fn (.slice input (count delimiter) (- (.-length input) (count delimiter)))))
               (do (vreset! state ::in-code)
                   (vswap! in-code conj (.slice input (count delimiter)))
                   result))

             ; end
             (and (= @state ::in-code) (.endsWith input delimiter))
             (let [code (conj @in-code (.slice input 0 (- (.-length input) (count delimiter))))]
               (vreset! state ::start)
               (vreset! in-code [])
               (xf result (result-fn (string/join " " code))))

             (= @state ::in-code) (do (vswap! in-code conj input) result)

             :else (xf result input))
           (xf result input)))))))

(def extract-code-blocks
  (make-delimited-processor {:delimiter "```"
                             :result-fn (fn [input]
                                       (dom/code #js {:className "multiline-code"} input))}))
(def extract-code-inline
  (make-delimited-processor {:delimiter "`"
                             :result-fn (fn [input]
                                       (dom/code #js {:className "inline-code"}
                                                 input))}))

(defn format-message
  "Given the text of a message body, turn it into dom nodes, making urls into
  links"
  [text]
  (let [stateless-transform (map (comp
                                   url-replace
                                   user-replace
                                   tag-replace
                                   emoji-shortcodes-replace
                                   emoji-ascii-replace))
        statefull-transform (comp extract-code-blocks extract-code-inline)]
    (->> (into [] (comp statefull-transform stateless-transform) (string/split text #" "))
         (interleave (repeat " "))
         rest)))

(defn format-date
  "Turn a Date object into a nicely formatted string"
  [datetime]
  (let [datetime (t/to-default-time-zone datetime)
        now (t/to-default-time-zone (t/now))
        format (cond
                 (= (f/unparse (f/formatter "yyyydM") now)
                    (f/unparse (f/formatter "yyyydM") datetime))
                 "h:mm A"

                 (= (t/year now) (t/year datetime))
                 "h:mm A MMM d"

                 :else
                 "h:mm A MMM d yyyy")]
    (f/unparse (f/formatter format) datetime)))
