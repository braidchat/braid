(ns braid.client.ui.views.message
  (:require [reagent.core :as r]
            [clojure.string :as string]
            [re-frame.core :refer [dispatch subscribe]]
            [braid.client.helpers :as helpers :refer [id->color ->color]]
            [braid.client.ui.views.embed :refer [embed-view]]
            [braid.client.ui.views.pills :refer [tag-pill-view user-pill-view]]
            [braid.client.emoji :as emoji]
            [braid.client.routes :as routes]))

(defn abridged-url
  "Given a full url, returns 'domain.com/*.png' where"
  [url]
  (let [char-limit 30
        {:keys [domain path]} (helpers/url->parts url)]
    (let [url-and-path (str domain path)]
      (if (> char-limit (count url-and-path))
        url-and-path
        (let [gap "/..."
              path-char-limit (- char-limit (count domain) (count gap))
              abridged-path (apply str (take-last path-char-limit path))]
          (str domain gap abridged-path))))))

(defn link-pill-view [url]
  [:a.external {:href url
                :title url
                :style {:background-color (helpers/url->color url)
                        :border-color (helpers/url->color url)}
                :on-click (fn [e] (.stopPropagation e))
                :target "_blank"
                ; rel to address vuln caused by target=_blank
                ; https://www.jitbit.com/alexblog/256-targetblank---the-most-underestimated-vulnerability-ever/
                :rel "noopener noreferrer"
                :tabIndex -1}
   (abridged-url url)])

(def replacements
  {:urls
   {:pattern helpers/url-re
    :replace (fn [url]
               [link-pill-view url])}
   :users
   {:pattern #"@([-0-9a-z]+)"
    :replace (fn [match]
               ;TODO: Subscribe to valid user id
               (if (some? @(subscribe [:user (uuid match)]))
                 [user-pill-view (uuid match)]
                 [:span "@" match]))}
   :tags
   {:pattern #"#([-0-9a-z]+)"
    :replace (fn [match]
               (if (some? @(subscribe [:tag (uuid match)]))
                 [tag-pill-view (uuid match)]
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
             (and (= @state ::start) (string/starts-with? input delimiter))
             (cond
               (and (not= input delimiter) (string/ends-with? input delimiter))
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
             (and (= @state ::in-code) (string/ends-with? input delimiter))
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
                             :result-fn (fn [body]
                                          [:code.prettyprint.multiline-code.lang-clj body])}))
(def extract-code-inline
  (make-delimited-processor {:delimiter "`"
                             :result-fn (fn [body]
                                          [:code.prettyprint.inline-code.lang-clj body])}))

(def extract-emphasized
  (make-delimited-processor {:delimiter "*"
                             :result-fn (fn [body] [:strong.starred body])}))

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

(defn message-view [message embed-update-chan]
  (let [sender (subscribe [:user (message :user-id)])
        current-group (subscribe [:open-group-id])]
    (r/create-class
      {:component-did-mount
       (fn []
         ; TODO: use prettyPrintOne to only do the content of this node
         ; TODO: also call on IDidUpdate?
         ; TODO: don't call if don't have code?
         (when-let [PR (aget js/window "PR")]
           ((aget PR "prettyPrint"))))
       :reagent-render
       (fn [message embed-update-chan]
         (let [sender-path (if (@sender :bot?)
                             (routes/bots-path {:group-id @current-group})
                             (routes/search-page-path {:group-id @current-group
                                                       :query (str "@" (@sender :nickname))}))]
           [:div.message {:class (str " " (when (:collapse? message) "collapse")
                                      " " (if (:unseen? message) "unseen" "seen")
                                      " " (when (:first-unseen? message) "first-unseen")
                                      " " (when (:failed? message) "failed-to-send"))}
            (when (:failed? message)
              [:div.error
               [:span "Message failed to send"]
               [:button {:on-click
                         (fn [_] (dispatch [:resend-message message]))}
                "Resend"]])
            [:a.avatar {:href sender-path
                        :tabIndex -1}
             [:img {:src (@sender :avatar)
                    :style {:backgroundColor (id->color (@sender :id))}}]]
            [:div.info
             (when (@sender :bot?)
               [:span.bot-notice "BOT"])
             [:a.nickname {:tabIndex -1
                           :href sender-path}
              (@sender :nickname)]
             [:span.time {:title (message :created-at)}
              (helpers/format-date (message :created-at))]]

            (into [:div.content] (format-message (message :content)))

            (when-let [url (first (helpers/extract-urls (message :content)))]
              [embed-view url embed-update-chan])]))})))

