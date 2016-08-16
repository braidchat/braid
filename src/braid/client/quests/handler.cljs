(ns braid.client.quests.handler)

(def quests
  [; conversations

   {:id :quest/conversation-new
    :name "Start a conversation"
    :icon \uf0e6
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-tag
    :name "Tag a conversation"
    :icon \uf02c
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-reply
    :name "Reply to a conversation"
    :icon \uf112
    :listener (fn [state [event args]]
                (= event :new-message))}

   {:id :quest/conversation-private
    :name "Start a private conversation"
    :icon \uf21b

    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-mute
    :name "Close a conversation"
    :icon \uf00d
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-mute
    :name "Mute a conversation"
    :icon \uf070
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-close-ctrlx
    :name "Close a conversation with CTRL X"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-close-esc
    :name "Close a conversation with ESC"
    :listener (fn [state [event args]]
                false)}

   ; recent

   {:id :quest/recent-view
    :name "View your recent closed messages"
    :icon \uf1da
    :listener (fn [state [event args]]
                false)}

   ; messages

   {:id :quest/message-emoji
    :name "Send a message with an emoji"
    :icon \uf118
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-link
    :name "Send a message with a link"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-mention-user
    :name "Mention a user in a message"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-mention-tag
    :name "Mention a tag in a message"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-upload-file-button
    :name "Upload a file (via button)"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-upload-file-drag
    :name "Upload a file (via drag n drop)"
    :listener (fn [state [event args]]
                false)}

   ; search

   {:id :quest/search-word
    :name "Search for an old conversation by word"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/search-tag
    :name "Search for an old conversation by tag"
    :listener (fn [state [event args]]
                false)}

   ; profile

   {:id :quest/set-avatar
    :name "Set your avatar"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/set-profile
    :name "Set your profile"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/update-nickname
    :name "Update your nickname"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/verify-email
    :name "Verify your email"
    :listener (fn [state [event args]]
                false)}

   ; invite

   {:id :quest/invite
    :name "Invite a user to your group"
    :listener (fn [state [event args]]
                false)}

   ; tags

   {:id :quest/tags-review
    :name "Review your subscriptions"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-subscribe
    :name "Subscribe to a tag"
    :listener (fn [state [event args]]
                false)
    }
   {:id :quest/tag-unsubscribe
    :name "Unsubscribe from a tag"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-create
    :name "Create a tag"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-create-autocomplete
    :name "Create a tag using the autocomplete"
    :listener (fn [state [event args]]
                false)}

   {:id :quest/archives
    :name "Look into a tag's archives"
    :listener (fn [state [event args]]
                false)}

   ; settings

   {:id :review-digest-options
    :name "Review your email preferences"
    :listener (fn [state [event args]]
                false)}

   ; bots

   {:id :quest/bot-add
    :name "Add a bot to your group"
    :listener (fn [state [event args]]
                false)}

   ; clients

   {:id :quest/desktop-client
    :name "Try the Braid desktop app"
    :listener (fn [state [event args]]
                false)}
   {:id :quest/mobile-client
    :name "Try the Braid mobile app"
    :listener (fn [state [event args]]
                false)}
   {:id :quest/web-client
    :name "Try the Braid web app"
    :listener (fn [state [event args]]
                false)}

   ; groups

   {:id :quest/groups-create
    :name "Create another group"
    :listener (fn [state [event args]]
                false)}
   {:id :quest/groups-explore
    :name "Explore the available public groups"
    :listener (fn [state [event args]]
                false)}
   {:id :quest/groups-join-public
    :name "Join a public group"
    :listener (fn [state [event args]]
                false)}])

(defn quests-handler [state [event args]]
  (let [new-completed-quest-ids (->> quests
                                     (map (fn [quest]
                                            (when ((quest :listener) state [event args])
                                              (quest :id))))
                                     (remove nil?))]
    (update-in state [:user :completed-quest-ids] (fn [s]
                                                    (apply
                                                      (partial conj s)
                                                      new-completed-quest-ids)))))
