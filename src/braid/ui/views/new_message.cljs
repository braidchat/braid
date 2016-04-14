(ns braid.ui.views.new-message
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color debounce]]
            [braid.ui.views.autocomplete :refer [engines]])
  (:import [goog.events KeyCodes]))

(defn- resize-textbox [el]
  (set! (.. el -style -height) "auto")
  (set! (.. el -style -height)
        (str (min 300 (.-scrollHeight el)) "px")))



(defn textarea-view [config autocomplete-on-key-down]
  (let [connected? (subscribe [:connected?])

        state (r/atom {:text ""})

        clear-text! (fn [] (swap! state assoc :text ""))

        resize-textbox! (fn [] ) ; TODO

        send-message!
        (fn []
          (println "SEND_MESSAGE")
          (store/set-new-thread! (config :thread-id))
          (dispatch! :new-message {:thread-id (config :thread-id)
                                   :content (@state :text)
                                   :mentioned-user-ids (config :mentioned-user-ids)
                                   :mentioned-tag-ids (config :mentioned-tag-ids)}))

        set-text! (fn [text]
                    (swap! state assoc :text (.slice text 0 5000)))
        ]

    (r/create-class
      {:component-did-mount
       (fn [c]
         (resize-textbox (r/dom-node c))
         (.. js/window (addEventListener "resize" (fn [_]
                                                    (resize-textbox c)))))

       :component-did-update
       (fn [c]
         (resize-textbox (r/dom-node c)))

       :reagent-render
       (fn []
         [:textarea {:placeholder (config :placeholder)
                     :value (@state :text)
                     :disabled (not @connected?)
                     :on-change (fn [e]
                                  (set-text! (.. e -target -value))
                                  (resize-textbox (.. e -target))
                                  ;TODO tell autocomplete: :force-close? false
                                  )
                     :on-key-down (autocomplete-on-key-down
                                    {:on-submit (fn []
                                                  (send-message!)
                                                  (clear-text!))})}])})))

(defn autocomplete-results-view [{:keys [results highlighted-result-index on-click]}]
  [:div.autocomplete
   (if (seq results)
     [:div
      (map-indexed
        (fn [i result]
          [:div.result
           {:class (when (= i highlighted-result-index) "highlight")
            :style {:cursor "pointer"}
            :on-click (on-click result)}
           ((result :html))])
        results)]
     [:div.result
      "No Results"])])



(defn wrap-autocomplete [{:keys [config textarea-view results-view]}]
  (let [autocomplete-chan (chan)
        kill-chan (chan)
        throttled-autocomplete-chan (debounce autocomplete-chan 100)


        state (r/atom {:force-close? false
                       :highlighted-result-index 0
                       :results nil})

        reset-state! (fn []
                       (swap! state assoc
                              :force-close? false
                              :highlighted-result-index 0))

        highlight-next!
        (fn []
          (swap! state (fn [state]
                         (assoc state :highlighted-result-index
                           (mod (inc (state :highlighted-result-index))
                                (count (state :results)))))))

        highlight-prev!
        (fn []
          (swap! state (fn [state]
                         (assoc state :highlighted-result-index
                           (mod (dec (state :highlighted-result-index))
                                (count (state :results)))))))

        highlight-first!
        (fn []
          (swap! state assoc :highlighted-result-index 0))

        set-results!
        (fn [results]
          (swap! state assoc :results results))

        autocomplete-open? (fn [] (and
                                    (not (@state :force-close?))
                                    (not (nil? (@state :results)))))

        set-force-close! (fn []
                           (swap! state assoc :force-close? true))

        choose-result!
        (fn [result]
          ((result :action) (config :thread-id))
          ;TODO:
          #_(set-state! :text ((result :message-transform) text)))

        focus-textbox! (fn []
                         ;TODO
                         )

        ; returns a function that can be used in a textarea's :on-key-down handler
        ; takes a callback fn, the textareas intended submit action
        autocomplete-on-key-down
        (fn [{:keys [on-submit]}]
          (fn [e]
            (condp = e.keyCode
              KeyCodes.ENTER
              (cond
                ; ENTER when autocomplete -> trigger chosen result's action (or exit autocomplete if no result chosen)
                autocomplete-open?
                (do
                  (.preventDefault e)
                  (if-let [result (nth (@state :results) (@state :highlighted-result-index) nil)]
                    (choose-result! result)
                    (set-force-close!)))

                ; ENTER otherwise -> send message
                (not e.shiftKey)
                (do
                  (.preventDefault e)
                  (reset-state!)
                  (on-submit)))

              KeyCodes.ESC
              (set-force-close!)

              KeyCodes.UP
              (when autocomplete-open?
                (.preventDefault e)
                (highlight-prev!))

              KeyCodes.DOWN
              (when autocomplete-open?
                (.preventDefault e)
                (highlight-next!))

              nil)))
        ]

    (r/create-class
      {:component-will-mount
       (fn []
         (go (loop []
               (let [[v ch] (alts! [throttled-autocomplete-chan kill-chan])]
                 (when (= ch throttled-autocomplete-chan)
                   (set-results! (seq (mapcat (fn [e] (e v (config :thread-id))) engines)))
                   (highlight-first!)
                   (recur))))))

       :component-will-unmount
       (fn []
         (put! kill-chan (js/Date.)))

       :reagent-render
       (fn []
         [:div.autocomplete-wrapper
          [textarea-view config autocomplete-on-key-down]
          (when autocomplete-open?
            [autocomplete-results-view {:results
                                        (@state :results)

                                        :highlighted-result-index
                                        (@state :highlighted-result-index)

                                        :on-click
                                        (fn [result]
                                          (fn []
                                            (choose-result! result)
                                            (focus-textbox!)))}])])})))

(defn new-message-view [config]
  [:div.message.new
   [textarea-view config (fn [{:keys [on-submit]}]
                           (fn [e]
                             (condp = e.keyCode
                               KeyCodes.ENTER
                               (on-submit)
                               nil)))]
   #_[wrap-autocomplete {:config config
                       :textarea-view textarea-view
                       :results-view autocomplete-results-view}]])

