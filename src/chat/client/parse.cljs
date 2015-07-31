(ns chat.client.parse
  (:require [clojure.string :as string]
            [chat.client.store :as store]))

(def tag-pattern
  "Pattern to extract tags.  Would prefer for the first subpattern to be a
  zero-width assertion, but javascript doesn't support lookbehind. The last
  subpattern needs to be zero-width so that two adjacent tags will be removed
  for the tagless text. The tag name itself is the first capture group."
  #"(?:^|\s)#(\S+)(?=\s|$)")

(defn parse-tags
  "Given some text, extract the tags.  Returns a list of three elements: The
  valid (i.e. already existing) tag names, the text with all valid tags
  removed, and a set of found tags that are ambigiuous (i.e. multiple groups
  have a tag with that name)"
  [text]
  (let [avail-tags (->> (@store/app-state :tags) vals (map :name) set)
        tags (->> (re-seq tag-pattern text)
                  (map second)
                  (filter avail-tags))
        tagless-text (string/replace text tag-pattern
                                     (fn [m] (if (avail-tags (subs (string/trim m) 1)) "" m)))]
    [tags tagless-text]))
