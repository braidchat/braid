(ns braid.core.client.ui.views.autocomplete
  (:require
   [braid.lib.color :as color]
   [braid.lib.upload :as upload]
   [braid.core.client.schema :as schema]
   [clj-fuzzy.metrics :as fuzzy]
   [clojure.string :as string]
   [goog.string :as gstring]
   [re-frame.core :refer [subscribe dispatch]])
  (:import
   (goog.events KeyCodes)))

; fn that returns results that will be shown if pattern matches
;    inputs:
;       text - current text of user's message
;       thread-id - id of the thread
;    output:
;       if no pattern matched, return nil
;       if a trigger pattern was matched, an array of maps, each containing:
;         :html - fn that returns html to be displayed for the result
;             inputs:
;                 none
;             output:
;                 html (as returned by (dom/*) functions)
;         :action - fn to be triggered when result picked
;             inputs:
;             output:
;                 none expected
;         :message-transform - fn to apply to text of message
;             inputs:
;                text
;             output:
;                text to replace message with

(defn normalize [s]
  (-> (string/lower-case s)
      (string/replace #"\s" "")))

(defn simple-matches?
  [m s]
  (not= -1 (.indexOf m s)))

(defn fuzzy-matches? [m s]
  (when (and (some? s) (some? m))
    (let [m (normalize m)
          s (normalize s)]
      (or (simple-matches? m s)
          (< (fuzzy/levenshtein m s) 2)))))

; ... @<user>  -> autocompletes user name
(defn user-autocomplete-engine [text]
  (let [pattern #"\B@(\S{0,})$"]
    (when-let [query (second (re-find pattern text))]
      (let [group-id (subscribe [:open-group-id])]
        (->> @(subscribe [:users-in-group @group-id])
             (filter (fn [u]
                       (fuzzy-matches? (u :nickname) query)))
             (map (fn [user]
                    {:key
                     (fn [] (user :id))
                     :action
                     (fn [])
                     :message-transform
                     (fn [text]
                       (string/replace text pattern (str "@" (user :nickname) " ")))
                     :html
                     (fn []
                       [:div.user.match
                        [:img.avatar {:src (upload/->path (user :avatar))}]
                        [:div.info
                         [:div.name (user :nickname)]
                         [:div.extra (user :status)]]])})))))))

; ... #<tag>   -> autocompletes tag
(defn tag-autocomplete-engine [text]
  (let [pattern #"\B#(\S{0,})$"]
    (when-let [query (second (re-find pattern text))]
      (let [open-group-id (subscribe [:open-group-id])
            group-tags @(subscribe [:tags-in-group @open-group-id])
            exact-match? (some #(= query (:name %)) group-tags)]
        (->> group-tags
             (filter (fn [t]
                       (fuzzy-matches? (t :name) query)))
             (map (fn [tag]
                    {:key
                     (fn [] (tag :id))
                     :action
                     (fn [])
                     :message-transform
                     (fn [text]
                       (string/replace text pattern (str "#" (tag :name) " ")))
                     :html
                     (fn []
                       [:div.tag.match
                        [:div.color-block
                         {:style
                          (merge
                            {:border-color (color/->color (tag :id))}
                            (when @(subscribe [:user-subscribed-to-tag? (tag :id)])
                              {:background-color (color/->color (tag :id))}))}]
                        [:div.info
                         [:div.name (tag :name)]
                         [:div.extra (or (tag :description)
                                         (gstring/unescapeEntities "&nbsp;"))]]])}))
             (cons (when-not (or exact-match? (string/blank? query))
                     (let [tag (merge (schema/make-tag)
                                      {:name query
                                       :group-id @open-group-id})]
                       {:key (constantly (tag :id))
                        :action
                             (fn []
                               (dispatch [:create-tag! {:tag tag}]))
                        :message-transform
                             (fn [text]
                               (string/replace text pattern (str "#" (tag :name) " ")))
                        :html
                             (fn []
                               [:div.tag.match
                                [:div.color-block
                                 {:style {:border-color (color/->color (tag :id))
                                          :background-color (color/->color (tag :id))}}]
                                [:div.info
                                 [:div.name (str "Create tag " (tag :name))]
                                 [:div.extra
                                  (:name @(subscribe [:group (tag :group-id)]))]]])})))
             (remove nil?)
             reverse)))))
