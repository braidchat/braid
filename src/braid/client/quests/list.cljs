(ns braid.client.quests.list)

(def quests
  [; conversations

   {:id :quest/conversation-new
    :name "Start a conversation"
    :description "Conversations are the heart of Braid. Type in the box in the bottom left corner and hit [Enter]."
    :icon \uf0e6
    :goal 3
    :listener (fn [state [event args]]
                (= event :quests/show-quest-instructions))}])

(def disabled-quests
  [; conversations

   {:id :quest/conversation-tag
    :name "Tag a conversation"
    :icon \uf02c
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-reply
    :name "Reply to a conversation"
    :icon \uf112
    :goal 3
    :listener (fn [state [event args]]
                (= event :new-message))}

   {:id :quest/conversation-private
    :name "Start a private conversation"
    :icon \uf21b
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-mute
    :name "Close a conversation"
    :icon \uf00d
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-mute
    :name "Mute a conversation"
    :icon \uf070
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-close-ctrlx
    :name "Close a conversation with CTRL X"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-close-esc
    :name "Close a conversation with ESC"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; recent

   {:id :quest/recent-view
    :name "View your recent closed messages"
    :icon \uf1da
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; messages

   {:id :quest/message-emoji
    :name "Send a message with an emoji"
    :icon \uf118
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-link
    :name "Send a message with a link"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-mention-user
    :name "Mention a user in a message"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-mention-tag
    :name "Mention a tag in a message"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-upload-file-button
    :name "Upload a file (via button)"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-upload-file-drag
    :name "Upload a file (via drag n drop)"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; search

   {:id :quest/search-word
    :name "Search for an old conversation by word"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/search-tag
    :name "Search for an old conversation by tag"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; profile

   {:id :quest/set-avatar
    :name "Set your avatar"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/set-profile
    :name "Set your profile"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/update-nickname
    :name "Update your nickname"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/verify-email
    :name "Verify your email"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; invite

   {:id :quest/invite
    :name "Invite a user to your group"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; tags

   {:id :quest/tags-review
    :name "Review your subscriptions"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-subscribe
    :name "Subscribe to a tag"
    :goal 3
    :listener (fn [state [event args]]
                false)
    }
   {:id :quest/tag-unsubscribe
    :name "Unsubscribe from a tag"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-create
    :name "Create a tag"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-create-autocomplete
    :name "Create a tag using the autocomplete"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/archives
    :name "Look into a tag's archives"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; settings

   {:id :review-digest-options
    :name "Review your email preferences"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; bots

   {:id :quest/bot-add
    :name "Add a bot to your group"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; clients

   {:id :quest/desktop-client
    :name "Try the Braid desktop app"
    :goal 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/mobile-client
    :name "Try the Braid mobile app"
    :goal 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/web-client
    :name "Try the Braid web app"
    :goal 3
    :listener (fn [state [event args]]
                false)}

   ; groups

   {:id :quest/groups-create
    :name "Create another group"
    :goal 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/groups-explore
    :name "Explore the available public groups"
    :goal 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/groups-join-public
    :name "Join a public group"
    :goal 3
    :listener (fn [state [event args]]
                false)}])
