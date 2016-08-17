(ns braid.client.quests.list)

(def quests
  [
   ; quests
   {:id :quest/quest-complete
    :name "Learn about quests"
    :description "These quests will teach you about the various features throughout Braid. This one is complete, so click 'Get New Quest'."
    :icon \uf091
    :video ""
    :goal 1
    :listener (fn [state [event data]]
                true)}

   ; conversations

   {:id :quest/conversation-new
    :name "Start a conversation"
    :description "To start a new conversation, click on the left-most conversation, type your message, and hit [Enter]."
    :icon \uf0e6
    :video "/images/quests/conversation-new.gif"
    :goal 3
    :listener (fn [state [event data]]
                (and
                  (= event :new-message)
                  (= (data :thread-id) (:new-thread-id state))))}

   {:id :quest/conversation-reply
    :name "Reply to a conversation"
    :description "To add another message to a conversation, type in the text-area at the bottom of the conversation and hit [Enter]."
    :icon \uf112
    :video "/images/quests/conversation-new.gif"
    :goal 3
    :listener (fn [state [event data]]
                (and
                  (= event :new-message)
                  (not= (data :thread-id) (:new-thread-id state))))}

   {:id :quest/conversation-close
    :name "Close a conversation"
    :description "Close a conversation by clicking the X in its top-right corner. A conversation will show up again when someone replies to it, so feel free to close them frequently."
    :icon \uf00d
    :video "/images/quests/conversation-new.gif"
    :goal 3
    :listener (fn [state [event data]]
                (and
                  (= event :hide-thread)
                  (not (data :local-only?))))}])

(def disabled-quests
  [
   ; visit

   {:id :quest/visit
    :name "Log in on 5 different days"
    :description "asdf"
    :icon \uf02c
    :goal 5
    :listener (fn [state [event args]]
                false)}

   ; conversations

   {:id :quest/conversation-tag
    :name "Tag a conversation"
    :description "asdf"
    :icon \uf02c
    :goal 3
    :listener (fn [state [event args]]
                false)}

   {:id :quest/conversation-private
    :name "Start a private conversation"
    :icon \uf21b
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
