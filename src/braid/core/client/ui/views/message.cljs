(ns braid.core.client.ui.views.message
  (:require
    [braid.core.hooks :as hooks]
    [braid.lib.color :as color]
    [braid.lib.url :as url]
    [braid.lib.date :as date]
    [braid.lib.upload :as upload]
    [braid.core.client.routes :as routes]
    [braid.core.client.ui.views.mentions :refer [tag-mention-view user-mention-view]]
    [braid.core.client.ui.views.card-border :refer [card-border-view]]
    [braid.core.client.ui.views.user-hover-card :refer [user-hover-card-view]]
    [braid.popovers.helpers :as popover]
    [cljsjs.highlight.langs.clojure]
    [cljsjs.highlight.langs.css]
    [cljsjs.highlight.langs.javascript]
    [cljsjs.highlight.langs.sql]
    [cljsjs.highlight.langs.yaml]
    [cljsjs.highlight]
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(defn abridged-url
  "Given a full url, returns 'domain.com/*.png' where"
  [url]
  (let [char-limit 30
        {:keys [domain path]} (url/url->parts url)]
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
                :style {:background-color (color/url->color url)
                        :border-color (color/url->color url)}
                :on-click (fn [e] (.stopPropagation e))
                :target "_blank"
                ; rel to address vuln caused by target=_blank
                ; https://www.jitbit.com/alexblog/256-targetblank---the-most-underestimated-vulnerability-ever/
                :rel "noopener noreferrer"
                :tab-index -1}
   (abridged-url url)])

(def replacements
  {:urls
   {:pattern url/url-re
    :replace (fn [url]
               [link-pill-view url])}
   :users
   {:pattern #"@([-0-9a-z]+)"
    :replace (fn [match]
               ;TODO: Subscribe to valid user id
               (if (some? @(subscribe [:user (uuid match)]))
                 [user-mention-view (uuid match)]
                 [:span "@" match]))}
   :tags
   {:pattern #"#([-0-9a-z]+)"
    :replace (fn [match]
               (if (some? @(subscribe [:tag (uuid match)]))
                 [tag-mention-view (uuid match)]
                 [:span "#" match]))}})

(defn re-replace
  [re s replace-fn]
  (if-let [match (second (re-find re s))]
    ; TODO: recurse, incase the rest has more matches?
    ; using Javascript split beacuse we don't want the match to be in the last
    ; component
    (let [[pre _ post] (seq (.split s re 3))]
      (if (or (string/blank? pre) (re-matches #".*\s$" pre))
      ; XXX: find a way to return a seq & use mapcat instead of this hack
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
             ;; TODO: handle starting code block with delimiter not at beginning of word

             ;; opening delimiter; initialize volatile! state variables
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

             ;; word ending with the closing delimiter
             (and (= @state ::in-code) (string/ends-with? input delimiter))
             (let [code (conj @in-code (.slice input 0 (- (.-length input) (count delimiter))))]
               (vreset! state ::start)
               (vreset! in-code [])
               (xf result (result-fn (string/join " " code))))

             ;; word that contains the closing delimiter and some additional text afterwards
             (and (= @state ::in-code) (not= -1 (.indexOf input delimiter)))
             (let [idx (.indexOf input delimiter)
                   code (conj @in-code (.slice input 0 idx))
                   after (.slice input (inc idx) (.-length input))]
               (vreset! state ::start)
               (vreset! in-code [])
               (reduce xf result [(result-fn (string/join " " code)) after]))

             ;; closing delimiter not yet found; eat next word
             (= @state ::in-code) (do (vswap! in-code conj input) result)

             ;; opening delimiter not yet found; pass word on to rest of xform
             :else (xf result input))

           ;; not a string, pass on to rest of xform
           (xf result input)))))))

(def url-replace (make-text-replacer :urls))
(def user-replace (make-text-replacer :users))
(def tag-replace (make-text-replacer :tags))

(defn code-view-gen [class-name]
  (fn [body]
    [:pre
     {:class class-name}
     [:code
      {:ref (fn [node]
              (when node
                (js/hljs.highlightBlock node)))}
      body]]))

(def extract-code-blocks
  (make-delimited-processor {:delimiter "```"
                             :result-fn (code-view-gen "multiline")}))

(def extract-code-inline
  (make-delimited-processor {:delimiter "`"
                             :result-fn (code-view-gen "inline")}))

(def extract-emphasized
  (make-delimited-processor {:delimiter "*"
                             :result-fn (fn [body] [:strong.starred body])}))

(defonce stateless-formatters
  (hooks/register! (atom []) [fn?]))

(defonce post-transformers
  (hooks/register! (atom []) [fn?]))

(defonce footer-views
  (hooks/register! (atom []) [fn?]))

(defn format-message
  "Given the text of a message body, turn it into dom nodes, making urls into
  links"
  [text]
  (let [; Caution: order of transforms is important! url-replace should come before
        ; user/tag replace at least so urls with octothorpes or at-signs don't get
        ; wrecked
        additional-formatters (reduce comp identity @stateless-formatters)
        stateless-transform (map (comp
                                   tag-replace
                                   user-replace
                                   url-replace
                                   additional-formatters))
        statefull-transform (comp extract-code-blocks
                                  extract-code-inline
                                  extract-emphasized)
        post-transform (fn [msg-body]
                         (reduce #(%2 %1) msg-body @post-transformers))]
    (->> (string/split text #" ")
        (into [] (comp statefull-transform stateless-transform))
        (interleave (repeat " "))
        rest
        post-transform)))

(defonce message-sender-views
  (hooks/register! (atom []) [fn?]))

(defn default-sender-views
  [sender-id]
  (let [sender @(subscribe [:user sender-id])]
    {:avatar [:span.avatar {:on-click (popover/on-mouse-enter
                                        (fn []
                                          [user-hover-card-view (:id sender)]))}
              [:img {:src (upload/->path (:avatar sender))
                     :style {:background-color (color/->color (:id sender))}}]]
     :info nil
     :name (if (nil? sender)
             [:span.nickname "[DELETED]"]
             [:span.nickname {:on-click (popover/on-mouse-enter
                                          (fn []
                                            [user-hover-card-view (:id sender)]))
                              :style {:cursor "pointer"}}
              (:nickname sender)])}))

(defn message-view
  [message]
  (let [current-group (subscribe [:open-group-id])
        current-user @(subscribe [:user-id])
        admin? @(subscribe [:current-user-is-group-admin?] [current-group])
        {:keys [avatar info name]} (some (fn [f] (f (message :user-id)))
                                         (conj @message-sender-views
                                               default-sender-views))]
    [:div.message {:class (str " " (when (:collapse? message) "collapse")
                               " " (if (:unseen? message) "unseen" "seen")
                               " " (when (:first-unseen? message) "first-unseen")
                               " " (when (:failed? message) "failed-to-send"))}

     [card-border-view (message :thread-id)]

     (when (:failed? message)
       [:div.error
        [:span "Message failed to send"]
        [:button {:on-click
                  (fn [_] (dispatch [:resend-message! message]))}
         "Resend"]])

     avatar
     [:div.info
      info
      name
      [:span.time {:title (message :created-at)}
       (date/smart-format-date (message :created-at))]]

     (into [:div.content] (format-message (message :content)))

     (when (or (= (message :user-id) current-user) admin?)
       [:button.delete
        {:on-click (fn [_]
                     (when (js/confirm (str "Delete this message?\n"
                                            "\"" (message :content) "\""))
                       (dispatch [:core/retract-message!
                                  {:thread-id (message :thread-id)
                                   :message-id (message :id)
                                   :remote? true}])))}
        \uf1f8])

     (into [:div.footer]
           (for [footer-view @footer-views]
             [footer-view message]))]))
