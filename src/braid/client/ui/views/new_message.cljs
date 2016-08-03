(ns braid.client.ui.views.new-message
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.ratom :refer-macros [run!]]
            [braid.client.state :refer [subscribe]]
            [cljs.core.async :as async :refer [<! put! chan alts!]]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.store :as store]
            [braid.client.s3 :as s3]
            [braid.client.helpers :refer [debounce stop-event!]]
            [braid.client.ui.views.autocomplete :refer [engines]])
  (:import [goog.events KeyCodes]))

(defn- resize-textbox [el]
  (set! (.. el -style -height) "auto")
  (set! (.. el -style -height)
        (str (+ 5 (min 300 (.-scrollHeight el))) "px")))

(defn textarea-view [{:keys [text set-text! config on-key-down on-change]}]
  (let [this-elt (r/atom nil)
        connected? (subscribe [:connected?])
        focused? (subscribe [:thread-focused? (config :thread-id)])
        send-message!
        (fn [config text]
          (dispatch! :new-message {:thread-id (config :thread-id)
                                   :group-id (config :group-id)
                                   :content text
                                   :mentioned-user-ids (config :mentioned-user-ids)
                                   :mentioned-tag-ids (config :mentioned-tag-ids)}))
        _ (run! (do
                  (when (and @focused? @this-elt)
                    (.focus @this-elt))))]

    (r/create-class
      {:display-name "textarea"
       :component-did-mount
       (fn [c]
         (resize-textbox (r/dom-node c))
         (reset! this-elt (r/dom-node c)))

       :component-did-update
       (fn [c]
         (resize-textbox (r/dom-node c)))

       :reagent-render
       (fn [{:keys [text set-text! config on-key-down on-change]}]
         [:textarea {:placeholder (config :placeholder)
                     :value text
                     :disabled (not @connected?)
                     :on-focus (fn [e]
                                 (dispatch! :focus-thread (config :thread-id)))
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

(defn inside-code-block?
  [txt]
  (odd? (count (re-seq #"`" txt))))

(defn wrap-autocomplete [config]
  (let [autocomplete-chan (chan)
        kill-chan (chan)
        throttled-autocomplete-chan (debounce autocomplete-chan 100)

        thread-text-kill-chan (chan)
        thread-text-chan (chan)
        throttled-thread-text-chan (debounce thread-text-chan 500)
        thread-id (r/atom (config :thread-id))
        thread-text (subscribe [:thread-new-message] [thread-id])

        ; delibrately closing over value of sub
        state (r/atom {:text @thread-text
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

        update-thread-text! (fn [thread-id text] (put! thread-text-chan
                                                       {:thread-id thread-id
                                                        :content text}))
        set-text! (fn [text]
                    (let [text (.slice text 0 5000)]
                      (swap! state assoc :text text)
                      (update-thread-text! @thread-id text)))

        update-text! (fn [f]
                       (swap! state update :text f)
                       (update-thread-text! @thread-id (@state :text)))

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
            (handle-text-change! (.. e -target -value))
            (on-change e)))]

    (r/create-class
      {:display-name "autocomplete"
       :component-will-mount
       (fn [c]
         (swap! state assoc :text @thread-text)
         (let [config (r/props c)]
           (go (loop []
                 (let [[text ch] (alts! [throttled-autocomplete-chan kill-chan])]
                   (when (= ch throttled-autocomplete-chan)
                     (when-not (inside-code-block? text)
                       (set-results!
                         (seq (mapcat (fn [e] (e text)) engines)))
                       (highlight-first!))
                     (recur)))))
           (go (loop []
                 (let [[v ch] (alts! [throttled-thread-text-chan
                                      thread-text-kill-chan])]
                   (if (= ch throttled-thread-text-chan)
                     (do
                       (dispatch! :new-message-text v)
                       (recur))
                     (dispatch! :new-message-text {:thread-id @thread-id
                                                   :content (@state :text)})))))))

       :component-will-update
       (fn [c [_ new-config]]
         (reset! thread-id (new-config :thread-id)))

       :component-will-unmount
       (fn []
         (put! kill-chan (js/Date.))
         (put! thread-text-kill-chan (js/Date.)))

       :reagent-render
       (fn [config]
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

(defn upload-button-view
  [config]
  (let [uploading? (r/atom false)]
    (fn [config]
      ; clicking on label == clicking on (hidden) input
      [:label.plus {:class (when @uploading? "uploading")}
       [:input {:type "file"
                :multiple false
                :style {:display "none"}
                :on-change (fn [e]
                             (reset! uploading? true)
                             (s3/upload
                               (aget (.. e -target -files) 0)
                               (fn [url]
                                 (reset! uploading? false)
                                 (dispatch! :create-upload
                                            {:url url
                                             :group-id (config :group-id)
                                             :thread-id (config :thread-id)}))))}]])))

(defn new-message-view [config]
  [:div.message.new
   [upload-button-view config]
   [wrap-autocomplete config]])

