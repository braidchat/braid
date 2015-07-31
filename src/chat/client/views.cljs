(ns chat.client.views
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [clojure.set :refer [union]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.parse :refer [extract-tags]]
            [chat.client.store :as store]))

(defn index-of
  "clojurescript doesn't get .indexOf as clojure does.  Return nil on not-found instead of -1"
  [coll v]
  (let [i (count (take-while (partial not= v) coll))]
    (when (< i (count coll))
      i)))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message"}
        (dom/img #js {:className "avatar" :src (get-in @store/app-state [:users (message :user-id) :avatar])})
        (dom/div #js {:className "content"}
          (message :content))))))

(defn tag->color [tag]
  ; normalized is approximately evenly distributed between 0 and 1
  (let [normalized (-> (tag :id)
                       str
                       (.substring 33 36)
                       (js/parseInt 16)
                       (/ 4096))]
    (str "hsl(" (* 360 normalized) ",60%,60%)")))

(defn make-tags-preview
  "Helper function for new-message-view to display a preview of typed tags"
  [tags owner]
  (apply dom/div #js {:className "tags-preview"}
    (map (fn [tag]
           (dom/div #js {:className "tag"
                         :style #js {:background-color (if-let [tag-id (tag :id)]
                                                         (tag->color {:id tag-id}))}}

             (when-let [possible-tags (tag :possibilities)]
             ; If the tag is ambiguous, show the groups it could be in
               (apply dom/div #js {:className "ambiguous"}
               (interpose
                 " or "
                 (map (fn [t]
                        (dom/span #js {:className "group"
                                       :onClick
                                       (fn [_]
                                         (om/update-state!
                                           owner [:ambiguous-tags (tag :idx)]
                                           (fn [amb-tag]
                                             (-> amb-tag
                                                 (dissoc :possibilities)
                                                 (assoc :id (t :id))))))}
                          (t :group-name)))
                      possible-tags))))

             (tag :name)))
         tags)))

(defn- set-state-for-text
  "Helper function to set the state (preview-tag-names and tags) of the
  new-message-view based on the input"
  [text owner]
  (om/set-state! owner :text text)
  (let [tag-names (extract-tags text)
        {obvious-names true ambiguous-names false} (group-by (complement store/ambiguous-tag?) tag-names)]
    (om/set-state! owner :obvious-tags (map (fn [tag-name]
                                              {:name tag-name
                                               :id (store/tag-id-for-name tag-name)})
                                            obvious-names))
    ; setting the ambiguous tags is slightly complicated; we can't just replace
    ; the existing ambiguous tags, since it's possible the user has selected
    ; the tag they meant, which we want to retain
    (om/update-state! owner :ambiguous-tags
                      (fn [old-tags]
                        (loop [old-names (mapv :name old-tags)
                               to-add ambiguous-names
                               acc []
                               i 0]
                          (if (empty? to-add)
                            acc
                            (let [tag-name (first to-add)
                                  idx (index-of old-names tag-name)]
                              (if (nil? idx)
                                (recur old-names
                                       (rest to-add)
                                       (conj acc {:name tag-name
                                                  :possibilities (store/ambiguous-tag? tag-name)
                                                  :idx i})
                                       (inc i))
                                (recur (assoc old-names idx nil)
                                       (rest to-add)
                                       (conj acc (assoc (nth old-tags idx) :idx i))
                                       (inc i))))))))))

(defn new-message-view [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:obvious-tags []
       :ambiguous-tags []
       :text ""})
    om/IRenderState
    (render-state [_ {:keys [text] :as state}]
      (dom/div #js {:className "message new"}
        (dom/textarea #js {:placeholder (config :placeholder)
                           :value (state :text)
                           :onChange (fn [e] (set-state-for-text (.. e -target -value) owner))
                           :onKeyDown
                           (fn [e]
                             (when (and (= 13 e.keyCode) (= e.shiftKey false))
                               (dispatch! :new-message {:thread-id (config :thread-id)
                                                        :content text
                                                        :tag-ids (->> (map :id (concat (state :obvious-tags)
                                                                                       (state :ambiguous-tags)))
                                                                      (remove nil?)
                                                                      set)})
                               (.preventDefault e)
                               (om/set-state! owner {:obvious-tags [] :ambiguous-tags [] :text ""})))})
        (make-tags-preview (concat (state :obvious-tags) (state :ambiguous-tags))
                           owner)))))

(defn thread-tags-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (let [tags (->> (thread :tag-ids)
                      (map #(get-in @store/app-state [:tags %])))]
        (apply dom/div #js {:className "tags"}
          (map (fn [tag]
                 (dom/div #js {:className "tag"
                               :style #js {:background-color (tag->color tag)}}
                   (tag :name))) tags))))))

(defn thread-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (dom/div #js {:className "close"
                      :onClick (fn [_]
                                 (dispatch! :hide-thread {:thread-id (thread :id)}))} "×")
        (om/build thread-tags-view thread)
        (apply dom/div #js {:className "messages"}
          (om/build-all message-view (->> (thread :messages)
                                          (sort-by :created-at))
                        {:key :id}))
        (om/build new-message-view {:thread-id (thread :id) :placeholder "Reply..."})))))

(defn new-thread-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (om/build new-message-view {:placeholder "Start a new conversation..."})))))

(defn tag-view [tag owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "tag"
                    :onClick (fn [_]
                               (if (tag :subscribed?)
                                 (dispatch! :unsubscribe-from-tag (tag :id))
                                 (dispatch! :subscribe-to-tag (tag :id))))}
        (dom/div #js {:className "color-block"
                      :style #js {:backgroundColor (tag->color tag)}}
          (when (tag :subscribed?)
            "✔"))
        (dom/span #js {:className "name"}
          (tag :name))))))

(defn new-tag-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:className "new-tag"
                      :onKeyDown
                      (fn [e]
                        (when (= 13 e.keyCode)
                          (let [text (.. e -target -value)]
                            (dispatch! :create-tag [text (data :group-id)]))
                          (.preventDefault e)
                          (aset (.. e -target) "value" "")))
                      :placeholder "New Tag"}))))

(defn tag-group-view [group-tags owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "group-tags"}
        (dom/h2 #js {:className "group-name"}
          (get-in group-tags [1 0 :group-name]))
        (apply dom/div #js {:className "tags"}
          (om/build-all tag-view (second group-tags)))
        (om/build new-tag-view {:group-id (first group-tags)})))))

(defn tags-view [tags owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "tag-groups"}
        (om/build-all tag-group-view tags)))))


(defn chat-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [threads (vals (data :threads))
            tags (->> (data :tags)
                      vals
                      (map (fn [tag]
                             (assoc tag :subscribed?
                               (contains? (get-in @store/app-state [:user :subscribed-tag-ids]) (tag :id)))))
                      (group-by :group-id))]
        (dom/div nil
          (dom/div #js {:className "meta"}
            (dom/img #js {:className "avatar"
                          :src (let [user-id (get-in @store/app-state [:session :user-id])]
                                 (get-in @store/app-state [:users user-id :avatar]))})
            (dom/div #js {:className "extras"}
              (om/build tags-view tags)
              (dom/div #js {:className "logout"
                            :onClick (fn [_] (dispatch! :logout nil))} "Log Out")))
          (apply dom/div #js {:className "threads"}
            (concat (om/build-all thread-view threads {:key :id})
                    [(om/build new-thread-view {} {:react-key "new-thread"})])))))))

(defn login-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:email ""
       :password ""})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "login"}
        (dom/input
          #js {:placeholder "Email"
               :type "text"
               :value (state :email)
               :onChange (fn [e] (om/set-state! owner :email (.. e -target -value)))})
        (dom/input
          #js {:placeholder "Password"
               :type "password"
               :value (state :password)
               :onChange (fn [e] (om/set-state! owner :password (.. e -target -value)))})
        (dom/button
          #js {:onClick (fn [e]
                          (dispatch! :auth {:email (state :email)
                                            :password (state :password)}))}
          "Let's do this!")))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (if (data :session)
          (om/build chat-view data)
          (om/build login-view data))))))
