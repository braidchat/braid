(ns braid.core.client.ui.views.new-message
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [<! put! chan alts!]]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [run!]]
   [braid.core.hooks :as hooks]
   [braid.core.client.helpers :refer [debounce stop-event!]]
   [braid.core.client.store :as store]
   [braid.core.client.ui.views.new-message-action-button :refer [new-message-action-button-view]])
  (:import
   (goog.events KeyCodes)))

(defn- resize-textbox! [el]
  (set! (.. el -style -height) "auto")
  (let [new-height (str (min 300 (.-scrollHeight el)) "px")]
    (set! (.. el -style -height) new-height)
    ;; also change the height of the parent node
    ;; which has a fixed height and overflow: hidden
    ;; to prevent browser from firing an extra scroll event
    ;; because of the 2 resizes that occur in the above
    (set! (.. el -parentNode -style -height) new-height)))

(defn textarea-view [_]
  (let [this-elt (atom nil)]
    (r/create-class
      {:component-did-update
       (fn [this [_ old-props]]
         (when (not= (:text (r/props this))
                     (:text old-props))
           (resize-textbox! @this-elt))
         (when (and (not (:focused? old-props))
                    (:focused? (r/props this)))
           (.focus @this-elt)))

       :reagent-render
       (fn [{:keys [text set-text! config on-key-down on-change focused?]}]
         ;; wrap with a div that has a fixed height and overflow: hidden
         ;; so that the auto-resize behaviour of the textarea does not cause
         ;; double scroll events
         [:div.textarea {:style {:overflow "hidden"
                                 ;; height gets overriden by JS
                                 :height "0px"}}
          [:textarea
           {:placeholder (config :placeholder)
            :value text
            :ref (fn [x]
                   (when (and x (nil? @this-elt))
                     (reset! this-elt x)
                     (resize-textbox! @this-elt)
                     (when focused?
                       (.focus @this-elt))))
            :disabled (not @(subscribe [:connected?]))
            :on-focus (fn [e]
                        (dispatch [:focus-thread (config :thread-id)]))
            :on-change (on-change
                         {:on-change
                          (fn [e]
                            (set-text! (.. e -target -value)))})
            :on-key-down
            (on-key-down
              {:on-submit
               (fn [e]
                 (dispatch [:new-message
                            {:thread-id (config :thread-id)
                             :group-id (config :group-id)
                             :content text
                             :mentioned-user-ids (config :mentioned-user-ids)
                             :mentioned-tag-ids (config :mentioned-tag-ids)}])
                 (set-text! ""))})}]])})))

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

(defn inside-code-block?
  [txt]
  (odd? (count (re-seq #"`" txt))))

(defonce autocomplete-engines
  (hooks/register! (atom []) [fn?]))

(defn wrap-autocomplete [config]
  (let [autocomplete-chan (chan)
        kill-chan (chan)
        throttled-autocomplete-chan (debounce autocomplete-chan 100)

        thread-text-kill-chan (chan)
        thread-text-chan (chan)
        throttled-thread-text-chan (debounce thread-text-chan 500)
        ; safe to close over thread-id
        thread-id (config :thread-id)
        ; safe to close over new-message
        thread-text (config :new-message)

        ; deliberately closing over value of sub
        state (r/atom {:text thread-text
                       :force-close? false
                       :highlighted-result-index 0
                       :results nil
                       :pos 0})

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

        update-thread-text! (fn [thread-id text]
                              (put! thread-text-chan
                                    {:thread-id thread-id
                                     :content text}))
        set-text! (fn [text]
                    (let [text (.slice text 0 5000)]
                      (swap! state assoc :text text)
                      (update-thread-text! thread-id text)))

        update-text! (fn [f]
                       (let [{:keys [text pos]} @state
                             updated-text (f (.slice text 0 pos))
                             new-text (str updated-text (.slice text pos))]
                         (swap! state assoc :text new-text)
                         (update-thread-text! thread-id (@state :text))))

        choose-result!
        (fn [result]
          ((result :action))
          (set-force-close!)
          (set-results! nil)
          (update-text! (result :message-transform)))

        focus-textbox! (fn [])
                         ;TODO


        handle-text-change! (fn [cursor text]
                              (clear-force-close!)
                              (put! autocomplete-chan {:text text :pos cursor}))

        ; returns a function that can be used in a textarea's :on-key-down handler
        ; takes a callback fn, the textareas intended submit action
        autocomplete-on-key-down
        (fn [{:keys [on-submit]}]
          (fn [e]
            (if (not (autocomplete-open?))
              (when (and (= e.keyCode KeyCodes.ENTER) (not e.shiftKey))
                (stop-event! e)
                (reset-state!)
                (on-submit e))
              (condp = e.keyCode
                KeyCodes.ENTER
                (do
                  (stop-event! e)
                  (when-let [result (nth (@state :results)
                                         (@state :highlighted-result-index) nil)]
                    (choose-result! result))
                  (set-force-close!))

                KeyCodes.ESC
                (do (stop-event! e)
                    (set-force-close!))

                KeyCodes.UP
                (do (stop-event! e)
                  (highlight-prev!))

                KeyCodes.DOWN
                (do (stop-event! e)
                  (highlight-next!))

                nil))))

        autocomplete-on-change
        (fn [{:keys [on-change]}]
          (fn [e]
            (let [cursor-pos (.. e -target -selectionStart)]
              (handle-text-change! cursor-pos (.. e -target -value))
              (swap! state assoc :pos cursor-pos))
            (on-change e)))]

    (r/create-class
      {:display-name "autocomplete"
       :component-will-mount
       (fn [c]
         (let [config (r/props c)]
           (go (loop []
                 (let [[data ch] (alts! [throttled-autocomplete-chan kill-chan])]
                   (when (= ch throttled-autocomplete-chan)
                     (let [{:keys [text pos]} data
                           text (.slice text 0 pos)]
                       (when-not (inside-code-block? text)
                         (set-results!
                           (seq (mapcat (fn [e] (e text)) @autocomplete-engines)))
                         (highlight-first!)))
                     (recur)))))
           (go (loop []
                 (let [[v ch] (alts! [throttled-thread-text-chan
                                      thread-text-kill-chan])]
                   (if (= ch throttled-thread-text-chan)
                     (do
                       (dispatch [:new-message-text (merge v {:group-id (config :group-id)}) v])
                       (recur))
                     (dispatch [:new-message-text {:group-id (config :group-id)
                                                   :thread-id thread-id
                                                   :content (@state :text)}])))))))

       :component-will-unmount
       (fn []
         (put! kill-chan (js/Date.))
         (put! thread-text-kill-chan (js/Date.)))

       :reagent-render
       (fn [config]
         [:div.autocomplete-wrapper
          [textarea-view {:text (@state :text)
                          :config config
                          :focused? @(subscribe [:thread-focused? (config :thread-id)])
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
   [new-message-action-button-view config]
   [wrap-autocomplete config]])
