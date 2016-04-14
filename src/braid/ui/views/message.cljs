(ns braid.ui.views.message
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [braid.ui.views.embed :refer [embed-view]]
            [braid.ui.views.pills :refer [tag-pill-view user-pill-view]]
            [chat.client.emoji :as emoji]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.views.helpers :as helpers :refer [starts-with? ends-with?]]
            [chat.client.views.pills :refer [tag-view user-view]]
            [chat.client.routes :as routes]))

(def url-re #"(http(?:s)?://\S+(?:\w|\d|/))")

(defn abridged-url
  "Given a full url, returns 'domain.com/*.png' where"
  [url]
  (let [char-limit 30
        [domain path] (rest (re-find #"http(?:s)?://([^/]+)(.*)" url))]
    (let [url-and-path (str domain path)]
      (if (> char-limit (count url-and-path))
        url-and-path
        (let [gap "/..."
              path-char-limit (- char-limit (count domain) (count gap))
              abridged-path (apply str (take-last path-char-limit path))]
          (str domain gap abridged-path))))))

(def replacements
  {:urls
   {:pattern url-re
    :replace (fn [match]
               [:a.external {:href match
                             :title match
                             :target "_blank"
                             :tabIndex -1}
                 (abridged-url match)])}
   :users
   {:pattern #"@([-0-9a-z]+)"
    :replace (fn [match]
               ;TODO: Subscribe to valid user id
               (if (store/valid-user-id? (uuid match))
                 [user-view {:id (uuid match)} subscribe]
                 [:span "@" match]))}
   :tags
   {:pattern #"#([-0-9a-z]+)"
    :replace (fn [match]
               (if-let [tag (store/get-tag (uuid match))]
                 [tag-pill-view tag subscribe]
                 [:span "#" match]))}
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
      [:span.dummy pre (replace-fn match) post]
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
               (and (not= input delimiter) (ends-with? input delimiter))
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
             (and (= @state ::in-code) (ends-with? input delimiter))
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
                             :result-fn (partial [:code.prettyprint.multiline-code.lang-clj])}))
(def extract-code-inline
  (make-delimited-processor {:delimiter "`"
                             :result-fn (partial [:code.prettyprint.inline-code.lang-clj])}))

(def extract-emphasized
  (make-delimited-processor {:delimiter "*"
                             :result-fn (partial [:strong.starred])}))


(defn extract-urls
  "Given some text, returns a sequence of URLs contained in the text"
  [text]
  (map first (re-seq url-re text)))

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

(defn message-view [message opts]
  (let [sender (subscribe [:user (message :user-id)])]
    (r/create-class
      {:component-did-mount
       (fn []
         (when-let [PR (aget js/window "PR")]
           ((aget PR "prettyPrint"))))
       :reagent-render
       (fn []
         (let [sender-path (routes/user-page-path {:group-id (routes/current-group)
                                                   :user-id (@sender :id)})]
           [:div.message {:class (str " " (when (:collapse? opts) "collapse")
                                      " " (if (:unseen? message) "unseen" "seen")
                                      " " (when (:first-unseen? message) "first-unseen")
                                      " " (when (:failed? message) "failed-to-send"))}
            (when (:failed? message)
              [:div.error
               [:span "Message failed to send"]
               [:button {:on-click
                         (fn [_] (dispatch! :resend-message message))}
                "Resend"]])
            [:a.avatar {:href sender-path
                        :tabIndex -1}
             [:img {:src (@sender :avatar)
                    :style {:backgroundColor (id->color (@sender :id))}}]]
            [:div.info
             [:a.nickname {:tabIndex -1
                           :href sender-path}
              (@sender :nickname)]
             [:span.time {:title (message :created-at)} (helpers/format-date (message :created-at))]]

            (into [:div.content] (format-message (message :content)))

            (when-let [url (first (extract-urls (message :content)))]
              [embed-view url])]))})))

