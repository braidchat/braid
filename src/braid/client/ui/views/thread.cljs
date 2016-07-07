(ns braid.client.ui.views.thread
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [braid.client.reagent-adapter :refer [reagent->react subscribe]]
            [cljs.core.async :refer [chan put!]]
            [braid.client.store :as store]
            [braid.client.dispatcher :refer [dispatch!]]
            [braid.client.helpers :as helpers]
            [braid.client.s3 :as s3]
            [braid.client.ui.views.pills :refer [user-pill-view tag-pill-view]]
            [braid.client.ui.views.message :refer [message-view]]
            [braid.client.ui.views.new-message :refer [new-message-view]])
  (:import [goog.events KeyCodes]))

(def max-file-size (* 10 1024 1024))

(defn thread-tags-view [thread]
  (let [thread-id (r/atom (thread :id))
        tags (subscribe [:tags-for-thread] [thread-id])
        mentions (subscribe [:mentions-for-thread] [thread-id])]
    (r/create-class
      {:display-name "thread-tags-view"

       :component-will-update
       (fn [c [_ new-thread]]
         (reset! thread-id (new-thread :id)))

       :reagent-render
       (fn [thread]
         [:div.tags
          (doall
            (for [user @mentions]
              ^{:key (user :id)}
              [user-pill-view (user :id)]))
          (doall
            (for [tag @tags]
              ^{:key (tag :id)}
              [tag-pill-view (tag :id)]))])})))

(defn messages-view [thread]
  ; Closing over thread-id, but the only time a thread's id changes is the new
  ; thread box, which doesn't have messages anyway
  (let [messages (subscribe [:messages-for-thread (thread :id)])

        unseen? (fn [message thread] (> (:created-at message)
                                        (thread :last-open-at)))

        scroll-to-bottom!
        (fn [component]
          (when-let [messages (r/dom-node component)]
            (set! (.-scrollTop messages) (.-scrollHeight messages))))]
    (r/create-class
      {:display-name "thread"

       :component-did-mount scroll-to-bottom!

       :component-did-update scroll-to-bottom!

       :reagent-render
       (fn [thread]
         [:div.messages
          (let [sorted-messages
                (->> @messages
                     (sort-by :created-at)
                     (cons nil)
                     (partition 2 1)
                     (map (fn [[prev-message message]]
                            (assoc message
                              :unseen?
                              (unseen? message thread)
                              :first-unseen?
                              (and
                                (unseen? message thread)
                                (not (unseen? prev-message thread)))
                              :collapse?
                              (and
                                (= (:user-id message)
                                   (:user-id prev-message))
                                (> (* 2 60 1000) ; 2 minutes
                                   (- (:created-at message)
                                      (or (:created-at prev-message) 0)))
                                (not (helpers/contains-urls? (prev-message :content))))))))]
            (doall
              (for [message sorted-messages]
                ^{:key (message :id)}
                [message-view message])))])})))

(defn thread-view [thread]
  (let [state (r/atom {:dragging? false
                       :uploading? false
                       :focused? false})
        set-uploading! (fn [bool] (swap! state assoc :uploading? bool))
        set-focused! (fn [bool] (swap! state assoc :focused? bool))
        set-dragging! (fn [bool] (swap! state assoc :dragging? bool))

        thread-private? (fn [thread] (and
                                       (not (thread :new?))
                                       (empty? (thread :tag-ids))
                                       (seq (thread :mentioned-ids))))

        thread-limbo? (fn [thread] (and
                                     (not (thread :new?))
                                     (empty? (thread :tag-ids))
                                     (empty? (thread :mentioned-ids))))

        ; Closing over thread-id, but the only time a thread's id changes is the new
        ; thread box, which is always open
        open? (subscribe [:thread-open? (thread :id)])

        focus-chan (chan)

        maybe-upload-file!
        (fn [thread file]
          (if (> (.-size file) max-file-size)
            (store/display-error! :upload-fail "File to big to upload, sorry")
            (do (set-uploading! true)
                (s3/upload file (fn [url]
                                  (set-uploading! false)
                                  (dispatch! :create-upload
                                             {:url url
                                              :thread-id (thread :id)
                                              :group-id (thread :group-id)}))))))]

    (fn [thread]
      (let [{:keys [dragging? uploading? focused?]} @state
            new? (thread :new?)
            private? (thread-private? thread)
            limbo? (thread-limbo? thread)
            archived? (and (not @open?) (not new?))]

        [:div.thread
         {:class
          (string/join " " [(when new? "new")
                            (when private? "private")
                            (when limbo? "limbo")
                            (when focused? "focused")
                            (when dragging? "dragging")
                            (when archived? "archived")])

          :on-click (fn [e]
                      (let [sel (.getSelection js/window)
                            selection-size (- (.-anchorOffset sel) (.-focusOffset sel))]
                        (when (zero? selection-size)
                          (helpers/stop-event! e)
                          (put! focus-chan (js/Date.))
                          (dispatch! :mark-thread-read (thread :id)))))
          :on-focus
          (fn [e]
            (set-focused! true))

          :on-blur
          (fn [e]
            (set-focused! false)
            (dispatch! :mark-thread-read (thread :id)))

          :on-key-up
          (fn [e]
            (when (or (and
                        (= KeyCodes.X (.-keyCode e))
                        (.-ctrlKey e))
                      (= KeyCodes.ESC (.-keyCode e)))
              (helpers/stop-event! e)
              (dispatch! :hide-thread (thread :id))))

          :on-paste
          (fn [e]
            (let [pasted-files (.. e -clipboardData -files)]
              (when (< 0 (.-length pasted-files))
                (.preventDefault e)
                (maybe-upload-file! thread (aget pasted-files 0)))))

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
                (maybe-upload-file! thread (aget file-list 0)))))}

         (when limbo?
           [:div.notice "No one can see this conversation yet. Mention a @user or #tag in a reply."])

         (when private?
           [:div.notice
            "This is a private conversation." [:br]
            "Only @mentioned users can see it."])

         [:div.card
          [:div.head
           (when (not new?)
             [:div.controls
              (if @open?
                [:div.control.close
                 {:title "Close"
                  :on-click (fn [e]
                              ; Need to preventDefault & propagation when using
                              ; divs as controls, otherwise divs higher up also
                              ; get click events
                              (helpers/stop-event! e)
                              (dispatch! :hide-thread (thread :id)))}]
                [:div.control.unread
                 {:title "Mark Unread"
                  :on-click (fn [e]
                              ; Need to preventDefault & propagation when using
                              ; divs as controls, otherwise divs higher up also
                              ; get click events
                              (helpers/stop-event! e)
                              (dispatch! :reopen-thread (thread :id)))}])
              [:div.control.mute
               {:title "Mute"
                :on-click (fn [e]
                            ; Need to preventDefault & propagation when using
                            ; divs as controls, otherwise divs higher up also
                            ; get click events
                            (helpers/stop-event! e)
                            (dispatch! :unsub-thread
                                       {:thread-id (thread :id)}))}]])

           [thread-tags-view thread]]

          (when-not new?
            [messages-view thread])

          (when uploading?
            [:div.uploading-indicator "\uf110"])

          [new-message-view {:thread-id (thread :id)
                             :group-id (thread :group-id)
                             :become-focused-chan focus-chan
                             :new-thread? new?
                             :placeholder (if new?
                                            "Start a conversation..."
                                            "Reply...")
                             :mentioned-user-ids (if new? (thread :mentioned-ids) ())
                             :mentioned-tag-ids (if new? (thread :tag-ids) ())}]]]))))

