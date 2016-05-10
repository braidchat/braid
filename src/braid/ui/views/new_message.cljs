(ns braid.ui.views.new-message
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [chat.client.reagent-adapter :refer [subscribe]]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [debounce]]
            [braid.ui.views.autocomplete :refer [engines]])
  (:import [goog.events KeyCodes]))

(defn- resize-textbox [el]
  (set! (.. el -style -height) "auto")
  (set! (.. el -style -height)
        (str (+ 5 (min 300 (.-scrollHeight el))) "px")))

(defn textarea-view [{:keys [text set-text! config on-key-down on-change]}]
  (let [connected? (subscribe [:connected?])

        send-message!
        (fn [config text]
          (dispatch! :new-message {:thread-id (config :thread-id)
                                   :content text
                                   :mentioned-user-ids (config :mentioned-user-ids)
                                   :mentioned-tag-ids (config :mentioned-tag-ids)}))]

    (r/create-class
      {:display-name "textarea"
       :component-did-mount
       (fn [c]
         (resize-textbox (r/dom-node c))
         (let [{:keys [config]} (r/props c)]
           (when (and (not (config :new-thread?))
                   (= (config :thread-id) (store/get-new-thread)))
             (store/clear-new-thread!)
             (.focus (r/dom-node c)))))

       :component-did-update
       (fn [c]
         (resize-textbox (r/dom-node c)))

       :reagent-render
       (fn [{:keys [text set-text! config on-key-down on-change]}]
         [:textarea {:placeholder (config :placeholder)
                     :value text
                     :disabled (not @connected?)
                     :on-change (on-change
                                  {:on-change
                                   (fn [e]
                                     (set-text! (.. e -target -value))
                                     (resize-textbox (.. e -target)))})
                     :on-key-down (on-key-down
                                    {:on-submit (fn [e]
                                                  (send-message! config text)
                                                  (set-text! ""))})}])})))

(defn autocomplete-results-view [{:keys [results highlighted-result-index on-click]}]
  [:div.autocomplete
   (if (seq results)
     (doall
       (map-indexed
         (fn [i result]
           [:div.result
            {:key ((result :key))
             :class (when (= i highlighted-result-index) "highlight")
             :style {:cursor "pointer"}
             :on-click (on-click result)}
            ((result :html))])
         results))
     [:div.result
      "No Results"])])

(defn wrap-autocomplete [{:keys [config textarea-view results-view]}]
  (let [autocomplete-chan (chan)
        kill-chan (chan)
        throttled-autocomplete-chan (debounce autocomplete-chan 100)

        state (r/atom {:text ""
                       :force-close? false
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

        autocomplete-open? (fn []
                             (and
                               (not (@state :force-close?))
                               (not (nil? (@state :results)))))

        set-force-close! (fn []
                           (swap! state assoc :force-close? true))

        clear-force-close! (fn []
                             (swap! state assoc :force-close? false))

        set-text! (fn [text]
                    (swap! state assoc :text (.slice text 0 5000)))

        update-text! (fn [f]
                       (swap! state update-in [:text] f))

        choose-result!
        (fn [result]
          ((result :action))
          (set-force-close!)
          (set-results! nil)
          (update-text! (result :message-transform)))

        focus-textbox! (fn []
                         ;TODO
                         )

        handle-text-change! (fn [text]
                              (clear-force-close!)
                              (put! autocomplete-chan text))

        ; returns a function that can be used in a textarea's :on-key-down handler
        ; takes a callback fn, the textareas intended submit action
        autocomplete-on-key-down
        (fn [{:keys [on-submit]}]
          (fn [e]
            (condp = e.keyCode
              KeyCodes.ENTER
              (cond
                ; ENTER when autocomplete -> trigger chosen result's action
                ; (or exit autocomplete if no result chosen)
                (autocomplete-open?)
                (do
                  (.preventDefault e)
                  (when-let [result (nth (@state :results)
                                         (@state :highlighted-result-index) nil)]
                    (choose-result! result))
                  (set-force-close!))

                ; ENTER otherwise -> send message
                (not e.shiftKey)
                (do
                  (.preventDefault e)
                  (reset-state!)
                  (on-submit e)))

              KeyCodes.ESC
              (set-force-close!)

              KeyCodes.UP
              (when (autocomplete-open?)
                (.preventDefault e)
                (highlight-prev!))

              KeyCodes.DOWN
              (when (autocomplete-open?)
                (.preventDefault e)
                (highlight-next!))

              nil)))

        autocomplete-on-change
        (fn [{:keys [on-change]}]
          (fn [e]
            (handle-text-change! (.. e -target -value))
            (on-change e)))]

    (r/create-class
      {:display-name "autocomplete"
       :component-will-mount
       (fn [c]
         (let [{:keys [config]} (r/props c)]
           (go (loop []
                 (let [[v ch] (alts! [throttled-autocomplete-chan kill-chan])]
                   (when (= ch throttled-autocomplete-chan)
                     (set-results!
                       (seq (mapcat (fn [e] (e v (config :thread-id))) engines)))
                     (highlight-first!)
                     (recur)))))))

       :component-will-unmount
       (fn []
         (put! kill-chan (js/Date.)))

       :reagent-render
       (fn [{:keys [config textarea-view results-view]}]
         [:div.autocomplete-wrapper
          [textarea-view {:text (@state :text)
                          :config config
                          :on-key-down autocomplete-on-key-down
                          :on-change autocomplete-on-change
                          :set-text! set-text!}]
          (when (autocomplete-open?)
            [autocomplete-results-view {:results
                                        (@state :results)

                                        :highlighted-result-index
                                        (@state :highlighted-result-index)

                                        :on-click
                                        (fn [result]
                                          (fn [e]
                                            (choose-result! result)
                                            (focus-textbox!)))}])])})))

(defn new-message-view [config]
  [:div.message.new
   [wrap-autocomplete {:config config
                       :textarea-view textarea-view
                       :results-view autocomplete-results-view}]])

