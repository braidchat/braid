(ns braid.client.quests.list)

; listeners must filter based on event name to avoid infinite loops

(def quests
  [
   ; quests
   {:quest/order 0
    :quest/id :quest/quest-complete
    :quest/name "Learn about quests"
    :quest/description "These quests will teach you about the various features throughout Braid. This one is complete, so click 'Get New Quest'."
    :quest/icon \uf091
    :quest/video ""
    :quest/goal 0
    :quest/listener (fn [state [event data]]
                      false)}

   ; conversations

   {:quest/order 1
    :quest/id :quest/conversation-new
    :quest/name "Start a conversation"
    :quest/description "To start a new conversation, click on the left-most conversation, type your message, and hit [Enter]."
    :quest/icon \uf0e6
    :quest/video "/images/quests/conversation-new.gif"
    :quest/goal 3
    :quest/listener (fn [state [event data]]
                      (and
                        (= event :new-message)
                        (->>
                          (get-in state [:threads (data :thread-id) :messages])
                          count
                          (= 1))))}

   {:quest/order 2
    :quest/id :quest/conversation-reply
    :quest/name "Reply to a conversation"
    :quest/description "To add another message to a conversation, type in the text-area at the bottom of the conversation and hit [Enter]."
    :quest/icon \uf112
    :quest/video "/images/quests/conversation-reply.gif"
    :quest/goal 3
    :quest/listener (fn [state [event data]]
                      (and
                        (= event :new-message)
                        (->>
                          (get-in state [:threads (data :thread-id) :messages])
                          count
                          (not= 1))))}

   {:quest/order 3
    :quest/id :quest/conversation-close
    :quest/name "Close a conversation"
    :quest/description "Close a conversation by clicking the X in its top-right corner. A conversation will show up again when someone replies to it, so feel free to close them frequently."
    :quest/icon \uf00d
    :quest/video "/images/quests/conversation-close.gif"
    :quest/goal 3
    :quest/listener (fn [state [event data]]
                      (and
                        (= event :hide-thread)
                        (not (data :local-only?))))}])


(def quests-by-id
  (->> quests
       (reduce (fn [memo quest]
                 (assoc memo (quest :quest/id) quest)) {})))

(def disabled-quests
  [
   ; visit

   {:quest/id :quest/visit
    :quest/name "Log in on 5 different days"
    :quest/description "asdf"
    :quest/icon \uf02c
    :quest/goal 5
    :quest/listener (fn [state [event args]]
                      false)}

   ; conversations

   {:quest/id :quest/conversation-tag
    :quest/name "Tag a conversation"
    :quest/description "asdf"
    :quest/icon \uf02c
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/conversation-private
    :quest/name "Start a private conversation"
    :quest/icon \uf21b
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/conversation-mute
    :quest/name "Mute a conversation"
    :quest/icon \uf070
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/conversation-close-ctrlx
    :quest/name "Close a conversation with CTRL X"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/conversation-close-esc
    :quest/name "Close a conversation with ESC"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   ; recent

   {:quest/id :quest/recent-view
    :quest/name "View your recent closed messages"
    :quest/icon \uf1da
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   ; messages

   {:quest/id :quest/message-emoji
    :quest/name "Send a message with an emoji"
    :quest/icon \uf118
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/message-link
    :quest/name "Send a message with a link"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/message-mention-user
    :quest/name "Mention a user in a message"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/message-mention-tag
    :quest/name "Mention a tag in a message"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/message-upload-file-button
    :quest/name "Upload a file (via button)"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   {:quest/id :quest/message-upload-file-drag
    :quest/name "Upload a file (via drag n drop)"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

   ; search

   {:quest/id :quest/search-word
    :quest/name "Search for an old conversation by word"
    :quest/goal 3
    :quest/listener (fn [state [event args]]
                      false)}

{:quest/id :quest/search-tag
 :quest/name "Search for an old conversation by tag"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

; profile

{:quest/id :quest/set-avatar
 :quest/name "Set your avatar"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

{:quest/id :quest/set-profile
 :quest/name "Set your profile"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

{:quest/id :quest/update-nickname
 :quest/name "Update your nickname"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

{:quest/id :quest/verify-email
 :quest/name "Verify your email"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

; invite

{:quest/id :quest/invite
 :quest/name "Invite a user to your group"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

; tags

{:quest/id :quest/tags-review
 :quest/name "Review your subscriptions"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

{:quest/id :quest/tag-subscribe
 :quest/name "Subscribe to a tag"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)
 }
{:quest/id :quest/tag-unsubscribe
 :quest/name "Unsubscribe from a tag"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

{:quest/id :quest/tag-create
 :quest/name "Create a tag"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

{:quest/id :quest/tag-create-autocomplete
 :quest/name "Create a tag using the autocomplete"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

{:quest/id :quest/archives
 :quest/name "Look into a tag's archives"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

; settings

{:quest/id :review-digest-options
 :quest/name "Review your email preferences"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

; bots

{:quest/id :quest/bot-add
 :quest/name "Add a bot to your group"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

; clients

{:quest/id :quest/desktop-client
 :quest/name "Try the Braid desktop app"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}
{:quest/id :quest/mobile-client
 :quest/name "Try the Braid mobile app"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}
{:quest/id :quest/web-client
 :quest/name "Try the Braid web app"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}

; groups

{:quest/id :quest/groups-create
 :quest/name "Create another group"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}
{:quest/id :quest/groups-explore
 :quest/name "Explore the available public groups"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}
{:quest/id :quest/groups-join-public
 :quest/name "Join a public group"
 :quest/goal 3
 :quest/listener (fn [state [event args]]
                   false)}])
