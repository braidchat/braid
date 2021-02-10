(ns braid.core.client.ui.views.thread
  (:require
   [braid.lib.color :as color]
   [braid.lib.date :as date]
   [braid.lib.url :as url]
   [braid.core.client.helpers :as helpers]
   [braid.core.client.ui.views.card-border :refer [card-border-view]]
   [braid.core.client.ui.views.message :refer [message-view]]
   [braid.core.client.ui.views.new-message :refer [new-message-view]]
   [braid.core.client.ui.views.thread-header :refer [thread-header-view]]
   [cljsjs.resize-observer-polyfill]
   [clojure.string :as string]
   [goog.object :as o]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r])
  (:import
   (goog.events KeyCodes)))

(def max-file-size (* 10 1024 1024))

(defn messages-view [thread-id]
  ; Closing over thread-id, but the only time a thread's id changes is the new
  ; thread box, which doesn't have messages anyway
  (let [messages (subscribe [:messages-for-thread thread-id])

        last-open-at (subscribe [:thread-last-open-at thread-id])

        unseen? (fn [message] (> (:created-at message) @last-open-at))

        messages-node (atom nil)
        ref-cb (fn [node] (reset! messages-node node))

        at-bottom? (atom true)
        first-scroll? (atom true)

        old-last-msg (atom nil)

        check-at-bottom!
        (fn []
          (let [node @messages-node]
            (reset! at-bottom?
                    (> 25
                       (- (.-scrollHeight node)
                          (.-scrollTop node)
                          (.-clientHeight node))))))

        scroll-to-bottom!
        (fn [component]
          (when-let [node @messages-node]
            (when @at-bottom?
              (set! (.-scrollTop node)
                    (- (.-scrollHeight node)
                       (.-clientHeight node))))))]

    (r/create-class
      {:display-name "thread"

       :component-did-mount
       (fn [c]
         (reset! at-bottom? true)
         (scroll-to-bottom! c)
         (reset! old-last-msg (:id (last @messages)))

         (let [resize-observer
               (js/ResizeObserver. (fn [entries]
                                     (scroll-to-bottom! c)))]
           (.. resize-observer (observe @messages-node))))

       :component-did-update
       (fn [c _]
         (let [last-msg (last @messages)]
           (when (and (not= @old-last-msg (:id last-msg))
                      (= (:user-id last-msg) @(subscribe [:user-id])))
             (reset! at-bottom? true))
           (reset! old-last-msg (:id last-msg)))
         (scroll-to-bottom! c))

       :reagent-render
       (fn [thread-id]
         [:div.messages
          {:ref ref-cb
           :on-scroll (fn [_]
                        (if @first-scroll?
                          (reset! first-scroll? false)
                          (check-at-bottom!)))}
          (let [sorted-messages
                (->> @messages
                     (sort-by :created-at)
                     (cons nil)
                     (partition 2 1)
                     (map (fn [[prev-message message]]
                            (let [new-date?
                                  (or (nil? prev-message)
                                      (not= (date/format-date "yyyyMMdd" (:created-at message))
                                            (date/format-date "yyyyMMdd" (:created-at prev-message))))]
                            (assoc message
                              :unseen?
                              (unseen? message)
                              :first-unseen?
                              (and
                                (unseen? message)
                                (not (unseen? prev-message)))
                              :show-date-divider?
                              new-date?
                              :collapse?
                              (and
                                (not new-date?)
                                (= (:user-id message)
                                   (:user-id prev-message))
                                (> (* 2 60 1000) ; 2 minutes
                                   (- (:created-at message)
                                      (or (:created-at prev-message) 0)))
                                ; TODO should instead check if there was an embed triggered
                                (not (url/contains-urls? (prev-message :content)))))))))]
            (doall
              (for [message sorted-messages]
                ;; [BUG] Reagent 0.8.1 has a bug where metadata doesn't get
                ;; applied to fragments properly, so we need to set
                ;; the key in this way instead
                [:<> {:key (message :id)}
                 (when (message :show-date-divider?)
                   [:div.divider
                    (when (:unseen? message)
                      [:div.border {:style {:background (color/->color @(subscribe [:open-group-id]))}}])
                    [:div.date (date/format-date "yyyy-MM-dd" (message :created-at))]])
                 [message-view (assoc message :thread-id thread-id)]])))])})))

(defn thread-view [thread]
  (let [state (r/atom {:dragging? false
                       :uploading? false})
        set-uploading! (fn [bool] (swap! state assoc :uploading? bool))
        set-dragging! (fn [bool] (swap! state assoc :dragging? bool))
        maybe-upload-file!
        (fn [thread file]
          (if (> (.-size file) max-file-size)
            (dispatch [:braid.notices/display! [:upload-fail "File too big to upload, sorry" :error]])
            (do (set-uploading! true)
                (dispatch [:braid.uploads/upload!
                           {:file file
                            :group-id (thread :group-id)
                            :type "upload"
                            :on-complete
                            (fn [{:keys [url id]}]
                              (set-uploading! false)
                              (dispatch [:braid.uploads/create-upload!
                                         {:url url
                                          :upload-id id
                                          :thread-id (thread :id)
                                          :group-id (thread :group-id)}]))}]))))]

    (fn [thread]
      (let [{:keys [dragging? uploading?]} @state
            open? @(subscribe [:thread-open? (thread :id)])
            focused? @(subscribe [:thread-focused? (thread :id)])
            private? (and
                       (seq (thread :messages))
                       (empty? (thread :tag-ids))
                       (seq (thread :mentioned-ids)))
            limbo?  (and
                      (seq (thread :messages))
                      (empty? (thread :tag-ids))
                      (empty? (thread :mentioned-ids)))
            archived? (not open?)]

        [:div.thread
         {:class
          (string/join " " [(when private? "private")
                            (when limbo? "limbo")
                            (when focused? "focused")
                            (when dragging? "dragging")
                            (when archived? "archived")])

          :on-click
          (fn [e]
            (let [sel (.getSelection js/window)
                  selection-size (- (.-anchorOffset sel) (.-focusOffset sel))]
              (when (zero? selection-size)
                (dispatch [:focus-thread (thread :id)]))))

          :on-blur
          (fn [e]
            (dispatch [:mark-thread-read (thread :id)]))

          :on-key-down
          (fn [e]
            (when (and (= KeyCodes.ESC (.-keyCode e))
                       (string/blank? (thread :new-message)))
              (helpers/stop-event! e)
              (dispatch [:hide-thread! {:thread-id (thread :id)}])))

          :on-paste
          (fn [e]
            (let [pasted-files (.. e -clipboardData -files)]
              (when (< 0 (.-length pasted-files))
                (.preventDefault e)
                (maybe-upload-file! thread (o/get pasted-files 0)))))

          :on-drag-over
          (fn [e]
            (helpers/stop-event! e)
            (set-dragging! true))

          :on-drag-leave
          (fn [e]
            (set-dragging! false))

          :on-drop
          (fn [e]
            (.preventDefault e)
            (set-dragging! false)
            (let [file-list (.. e -dataTransfer -files)]
              (when (< 0 (.-length file-list))
                (maybe-upload-file! thread (o/get file-list 0)))))}

         (when limbo?
           [:div.notice "No one can see this conversation yet. Mention a @user or #tag in a reply."])

         (when private?
           [:div.notice
            "This is a private conversation." [:br]
            "Only @mentioned users can see it."])

         [:div.card
          [card-border-view (thread :id)]

          [thread-header-view thread]

          [messages-view (thread :id)]

          (when uploading?
            [:div.uploading-indicator "\uf110"])

          (if (:readonly thread)
            [:button.join
             {:on-click (fn [_] (dispatch [:core/join-public-group (thread :group-id)]))
              :style {:background-color (color/->color (thread :group-id))}}
             "Join Group to Reply"]
            [new-message-view {:thread-id (thread :id)
                               :group-id (thread :group-id)
                               :placeholder (if (empty? (:messages thread))
                                              "Start a conversation..."
                                              "Reply...")
                               :new-message (thread :new-message)}])]]))))
