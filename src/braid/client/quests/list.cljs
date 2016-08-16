(ns braid.client.quests.list)

(def quests
  [; conversations

   {:id :quest/conversation-new
    :name "Start a conversation"
    :icon \uf0e6
    :count 3
    :listener (fn [state [event args]]
                (= event :quests/show-quest-instructions))}

   {:id :quest/conversation-tag
    :name "Tag a conversation"
    :icon \uf02c
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-reply
    :name "Reply to a conversation"
    :icon \uf112
    :count 3
    :listener (fn [state [event args]]
                (= event :new-message))}

   {:id :quest/conversation-private
    :name "Start a private conversation"
    :icon \uf21b
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-mute
    :name "Close a conversation"
    :icon \uf00d
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-mute
    :name "Mute a conversation"
    :icon \uf070
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-close-ctrlx
    :name "Close a conversation with CTRL X"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-close-esc
    :name "Close a conversation with ESC"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; recent

   {:id :quest/recent-view
    :name "View your recent closed messages"
    :icon \uf1da
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; messages

   {:id :quest/message-emoji
    :name "Send a message with an emoji"
    :icon \uf118
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-link
    :name "Send a message with a link"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-mention-user
    :name "Mention a user in a message"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-mention-tag
    :name "Mention a tag in a message"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-upload-file-button
    :name "Upload a file (via button)"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/message-upload-file-drag
    :name "Upload a file (via drag n drop)"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; search

   {:id :quest/search-word
    :name "Search for an old conversation by word"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/search-tag
    :name "Search for an old conversation by tag"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; profile

   {:id :quest/set-avatar
    :name "Set your avatar"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/set-profile
    :name "Set your profile"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/update-nickname
    :name "Update your nickname"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/verify-email
    :name "Verify your email"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; invite

   {:id :quest/invite
    :name "Invite a user to your group"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; tags

   {:id :quest/tags-review
    :name "Review your subscriptions"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-subscribe
    :name "Subscribe to a tag"
    :count 3
    :listener (fn [state [event args]]
                false)
    }
   {:id :quest/tag-unsubscribe
    :name "Unsubscribe from a tag"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-create
    :name "Create a tag"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/tag-create-autocomplete
    :name "Create a tag using the autocomplete"
    :count 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/archives
    :name "Look into a tag's archives"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; settings

   {:id :review-digest-options
    :name "Review your email preferences"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; bots

   {:id :quest/bot-add
    :name "Add a bot to your group"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; clients

   {:id :quest/desktop-client
    :name "Try the Braid desktop app"
    :count 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/mobile-client
    :name "Try the Braid mobile app"
    :count 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/web-client
    :name "Try the Braid web app"
    :count 3
    :listener (fn [state [event args]]
                false)}

   ; groups

   {:id :quest/groups-create
    :name "Create another group"
    :count 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/groups-explore
    :name "Explore the available public groups"
    :count 3
    :listener (fn [state [event args]]
                false)}
   {:id :quest/groups-join-public
    :name "Join a public group"
    :count 3
    :listener (fn [state [event args]]
                false)}])
