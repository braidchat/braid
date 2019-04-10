(ns braid.core.client.ui.views.thread
  (:require
   [braid.core.client.helpers :as helpers]
   [braid.core.client.routes :as routes]
   [braid.core.client.ui.views.card-border :refer [card-border-view]]
   [braid.core.client.ui.views.mentions :refer [user-mention-view tag-mention-view]]
   [braid.core.client.ui.views.message :refer [message-view]]
   [braid.core.client.ui.views.new-message :refer [new-message-view]]
   [braid.core.hooks :as hooks]
   [braid.popovers.helpers :as popovers]
   [cljsjs.resize-observer-polyfill]
   [clojure.set :refer [difference]]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [run! reaction]])
  (:import
   (goog.events KeyCodes)))

(def max-file-size (* 10 1024 1024))

(defn tag-option-view
  [tag thread-id selected?]
  [:div.tag-option
   {:on-click (fn []
                (popovers/close!)
                (dispatch [:add-tag-to-thread {:thread-id thread-id
                                               :tag-id (tag :id)}]))
    :class (when selected? "selected")}
   [:div.rect {:style {:background (helpers/->color (tag :id))}}]
   [:span {:style {:color (helpers/->color (tag :id))}}
    "#" (tag :name)]])

(defn user-option-view
  [user thread-id selected?]
  [:div.user-option
   {:on-click (fn []
                (popovers/close!)
                (dispatch [:add-user-to-thread {:thread-id thread-id
                                                :user-id (user :id)}]))
    :class (when selected? "selected")}
   [:div.rect {:style {:background (helpers/->color (user :id))}}]
   [:span {:style {:color (helpers/->color (user :id))}}
    "@" (user :nickname)]])

(defn add-tag-user-popover-view
  [thread]
  (let [search-query (r/atom "")
        selected-column (r/atom 0)
        selected-row (r/atom 0)]
    (fn [thread]
      (let [thread-tag-ids (set (thread :tag-ids))
            thread-user-ids (set (thread :mentioned-ids))
            tags (reaction
                   (->> @(subscribe [:open-group-tags])
                       (remove (fn [tag]
                                 (contains? thread-tag-ids (tag :id))))
                       (filter (fn [tag]
                                 (or (string/blank? @search-query)
                                     (string/includes? (tag :name) @search-query))))
                       (sort-by :name)
                       vec))
            users (reaction
                    (->> @(subscribe [:users])
                        (remove (fn [user] (contains? thread-user-ids (user :id))))
                        (filter (fn [user]
                                  (or (string/blank? @search-query)
                                      (string/includes? (user :nickname) @search-query))))
                        (sort-by :nickname)
                        vec))
            selected (reaction (get-in [@tags @users] [@selected-column @selected-row]))]
        (run!
          (->> (get (mapv (comp dec count) [@tags @users]) @selected-column)
              (min @selected-row)
              (reset! selected-row)))
        [:div.add-mention-popup
         [:input.search
          {:placeholder "Search for tag/user"
           :auto-focus true
           :value @search-query
           :on-change (fn [e] (reset! search-query
                                     (.. e -target -value)))
           :on-key-down
           (fn [e] (when-let [k (#{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight"
                                 "Escape" "Enter"}
                                 (.-key e))]
                    (.preventDefault e)
                    (.stopPropagation e)
                    (case k
                      ("ArrowRight" "ArrowLeft")
                      (let [other-col (mod (inc @selected-column) 2)
                            other-count (count (get [@tags @users] other-col))]
                        (when-not (zero? other-count)
                          (swap! selected-column (comp #(mod % 2) inc))))

                      "ArrowUp"
                      (swap! selected-row (comp (partial max 0) dec))

                      "ArrowDown"
                      (swap! selected-row inc)

                      "Enter"
                      (do
                        (popovers/close!)
                        (case @selected-column
                          0 (dispatch [:add-tag-to-thread
                                       {:thread-id (thread :id)
                                        :tag-id (@selected :id)}])
                          1 (dispatch [:add-user-to-thread
                                       {:thread-id (thread :id)
                                        :user-id (@selected :id)}])))

                      "Escape" (popovers/close!))))}]
         [:div.search-results

          [:div.tag-list
           (if (seq @tags)
             (doall
               (for [tag @tags]
                 ^{:key (tag :id)}
                 [tag-option-view tag (thread :id) (= tag @selected)]))
             [:div.name "No unused tags matching"])]

          [:div.user-list
           (if (seq @users)
             (doall
               (for [user @users]
                 ^{:key (user :id)}
                 [user-option-view user (thread :id) (= user @selected)]))
             [:div.name "No unmentioned users matching"])]]]))))

(defn add-tag-button-view [thread]
  [:div.add
   [:span.pill {:on-click (popovers/on-click
                            (fn []
                              [add-tag-user-popover-view thread]))}
    "+"]])

(defn thread-tags-view [thread]
  [:div.tags
   (doall
     (for [user-id (thread :mentioned-ids)]
       ^{:key user-id}
       [user-mention-view user-id]))
   (doall
     (for [tag-id (thread :tag-ids)]
       ^{:key tag-id}
       [tag-mention-view tag-id]))
   (when-not (:readonly thread)
     [add-tag-button-view thread])])

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
                                      (not= (helpers/format-date "yyyyMMdd" (:created-at message))
                                            (helpers/format-date "yyyyMMdd" (:created-at prev-message))))]
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
                                (not (helpers/contains-urls? (prev-message :content)))))))))]
            (doall
              (for [message sorted-messages]
                ;; [BUG] Reagent 0.8.1 has a bug where metadata doesn't get
                ;; applied to fragments properly, so we need to set
                ;; the key in this way instead
                [:<> {:key (message :id)}
                 (when (message :show-date-divider?)
                   [:div.divider
                    [:div.date (helpers/format-date "yyyy-MM-dd" (message :created-at))]])
                 [message-view (assoc message :thread-id thread-id)]])))])})))

(def header-item-dataspec
  {:priority number?
   :view fn?})

(defonce thread-header-items
  (hooks/register! (atom []) [header-item-dataspec]))

(def thread-control-dataspec
  {:priority number?
   :view fn?})

(defonce thread-controls
  (hooks/register!
    (atom [{:view
            (fn [thread]
              [:div.control.mute
               {:title "Mute"
                :on-click (fn [e]
                            ; Need to preventDefault & propagation when using
                            ; divs as controls, otherwise divs higher up also
                            ; get click events
                            (helpers/stop-event! e)
                            (dispatch [:unsub-thread
                                       {:thread-id (thread :id)}]))}
               \uf1f6])
            :priority 0}])
    [thread-control-dataspec]))

(defn thread-header-view [thread]
  (into
    [:div.head]
    (conj
      (->> @thread-header-items
           (sort-by :priority)
           reverse
           (mapv (fn [el]
                   [(el :view) thread])))

      (when (not (thread :new?))
        [:div.controls
         [:div.main
          (if @(subscribe [:thread-open? (thread :id)])
            [:div.control.close
             {:title "Close"
              :tabIndex 0
              :role "button"
              :on-click (fn [e]
                          ; Need to preventDefault & propagation when using
                          ; divs as controls, otherwise divs higher up also
                          ; get click events
                          (helpers/stop-event! e)
                          (dispatch [:hide-thread {:thread-id (thread :id)}]))}
             \uf00d]
            [:div.control.unread
             {:title "Mark Unread"
              :tabindex 0
              :role "button"
              :on-click (fn [e]
                          ; Need to preventDefault & propagation when using
                          ; divs as controls, otherwise divs higher up also
                          ; get click events
                          (helpers/stop-event! e)
                          (dispatch [:reopen-thread (thread :id)]))}
             \uf0e2])]

         (into [:div.extras]
               (->> @thread-controls
                    (sort-by :priority)
                    reverse
                    (mapv (fn [el]
                            [(el :view) thread]))))])

      [thread-tags-view thread])))

(defn thread-view [thread]
  (let [state (r/atom {:dragging? false
                       :uploading? false})
        set-uploading! (fn [bool] (swap! state assoc :uploading? bool))
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
        focused? (subscribe [:thread-focused? (thread :id)])
        maybe-upload-file!
        (fn [thread file]
          (if (> (.-size file) max-file-size)
            (dispatch [:braid.notices/display! [:upload-fail "File too big to upload, sorry" :error]])
            (do (set-uploading! true)
                (dispatch [:braid.uploads/upload!
                           file
                           (fn [url]
                             (set-uploading! false)
                             (dispatch [:braid.uploads/create-upload!
                                        {:url url
                                         :thread-id (thread :id)
                                         :group-id (thread :group-id)}]))]))))]

    (fn [thread]
      (let [{:keys [dragging? uploading?]} @state
            new? (thread :new?)
            private? (thread-private? thread)
            limbo? (thread-limbo? thread)
            archived? (and (not @open?) (not new?))]

        [:div.thread
         {:class
          (string/join " " [(when new? "new")
                            (when private? "private")
                            (when limbo? "limbo")
                            (when @focused? "focused")
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
              (dispatch [:hide-thread {:thread-id (thread :id)}])))

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
          [card-border-view (thread :id)]

          [thread-header-view thread]

          (when-not new?
            [messages-view (thread :id)])

          (when uploading?
            [:div.uploading-indicator "\uf110"])

          (when-not (:readonly thread)
            [new-message-view {:thread-id (thread :id)
                               :group-id (thread :group-id)
                               :placeholder (if new?
                                              "Start a conversation..."
                                              "Reply...")
                               :new-message (thread :new-message)
                               :mentioned-user-ids (when new?
                                                     (thread :mentioned-ids))
                               :mentioned-tag-ids (when new?
                                                    (thread :tag-ids))}])]]))))
