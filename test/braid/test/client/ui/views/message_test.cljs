(ns braid.test.client.ui.views.message-test
  "Unit tests for functions defined by the
  braid.core.client.ui.views.message namespace"
  (:require
   [braid.core.client.ui.views.message :as message]
   [cljs.test :refer-macros [deftest is testing]]))

(deftest test-abridged-url
  (testing "Returns domain and path as-is when then are less than 30 chars"
    (is (= "localhost"
           (message/abridged-url "https://localhost:5555")))
    (is (= "braid.chat"
           (message/abridged-url "https://braid.chat")))
    (is (= "github.com/some-user/29-chars"
           (message/abridged-url "https://github.com:1234/some-user/29-chars?arg=1"))))
  (testing "Returns domain + abridged path when they are not less than 30 chars"
    (is (= "short.domain/...ally/long/path"
           (message/abridged-url "https://short.domain/really/and/i/mean/really/long/path"))))
  (testing "Returns domain + elipsis when the domain is not less than 30 chars"
    (is (= "really.and.i.mean.really.long.domain/..."
           (message/abridged-url "https://really.and.i.mean.really.long.domain/short/path")))))


(deftest test-re-replace
  (let [replace-fn (fn [_] "REPLACED")
        no-match-message "This message contains no matches"]
    (testing "Replaces single occurrance of text that matches the supplied regex and wraps in a span"
      (is (= [:span.dummy "This message contains " "REPLACED" " information"]
             (message/re-replace
              (get-in message/replacements [:emoji-shortcodes :pattern])
              "This message contains :secret: information"
              replace-fn)))
      (is (= [:span.dummy "This message contains a " "REPLACED" " user tag"]
             (message/re-replace
              (get-in message/replacements [:users :pattern])
              "This message contains a @some-user123 user tag"
              replace-fn)))
      (is (= [:span.dummy "This message contains a " "REPLACED" " tag"]
             (message/re-replace
              (get-in message/replacements [:tags :pattern])
              "This message contains a #some-tag tag"
              replace-fn)))
      (is (= [:span.dummy "This message contains a " "REPLACED" " url"]
             (message/re-replace
              (get-in message/replacements [:urls :pattern])
              "This message contains a https://braid.chat/some/path?some-arg=blah url"
              replace-fn))))
    (testing "Returns message when there is no match for the supplied regex"
      (is (= no-match-message
             (message/re-replace
              (get-in message/replacements [:emoji-shortcodes :pattern])
              no-match-message
              replace-fn))))
    (testing "Has strange behavior when multiple matches occur in the message"
      (is (= [:span.dummy "This message contains " "REPLACED" " emojis "]
             (message/re-replace
              (get-in message/replacements [:emoji-shortcodes :pattern])
              "This message contains :two: emojis :heart:, isn't that neat?"
              replace-fn))))))


(deftest test-make-text-replacer
  (testing "Returns argument if not a string"
    (doseq [replacer [message/url-replace message/user-replace
                      message/tag-replace message/emoji-shortcodes-replace]]
      (is (= 1234 (replacer 1234)))))
  (testing "Calls replacer when argument is a string"
    (is (= [:span.dummy "This message contains a " [:img {:class "emojione", :alt ":heart:", :title ":heart:", :src "//cdn.jsdelivr.net/emojione/assets/png/2764.png"}] " emoji"]
           (message/emoji-shortcodes-replace "This message contains a :heart: emoji")))))


(def ascii-emoji [:img {:class "emojione"}])
(def shortcode-emoji [:span.dummy "" [:img {:class "emojione"}] ""])

(deftest test-shortcode-emoji?
  (testing "returns true when a message part is valid shortcode emoji html"
    (is (message/shortcode-emoji? shortcode-emoji)))
  (testing "returns false when a message part is not valid shortcode emoji html"
    (is (not (message/shortcode-emoji? "some text")))
    (is (not (message/shortcode-emoji? [])))
    (is (not (message/shortcode-emoji? (assoc-in shortcode-emoji [2 1 :class] "some-class"))))
    (is (not (message/shortcode-emoji? (assoc-in shortcode-emoji [2 0] :span))))))


(deftest test-ascii-emoji?
  (testing "returns true when a message part is valid ascii emoji html"
    (is (message/ascii-emoji? ascii-emoji)))
  (testing "returns false when a message part is not valid ascii emoji html"
    (is (not (message/ascii-emoji? "some text")))
    (is (not (message/ascii-emoji? [])))
    (is (not (message/ascii-emoji? (assoc ascii-emoji 0 :span))))
    (is (not (message/ascii-emoji? (assoc-in ascii-emoji [1 :class] "some-class"))))))


(deftest test-format-emojis
  (let [single-emoji-message [shortcode-emoji]
        multi-emoji-message [shortcode-emoji " " ascii-emoji " " "\n\t\n" " " ascii-emoji]
        mixed-message [ascii-emoji " " "some" " " "text" " " shortcode-emoji]
        no-emoji-message ["some" " " "text"]
        contains-class (fn [class]
                         (fn [emoji]
                           (cond (message/shortcode-emoji? emoji)
                                 (< -1 (.indexOf (get-in emoji [2 1 :class]) class))
                                 (message/ascii-emoji? emoji)
                                 (< -1 (.indexOf (get-in emoji [1 :class]) class))
                                 :else false)))]
    (testing "adds the 'large' class to all emojis in emoji-only messages"
      (doseq [message [single-emoji-message multi-emoji-message]]
        (is (every? (contains-class "large")
                    (->> (message/format-emojis message)
                         (filter message/emoji?))))))
    (testing "returns the message unmodified if not emoji-only message"
      (doseq [message [mixed-message no-emoji-message]]
        (is (= message (message/format-emojis message)))))))
