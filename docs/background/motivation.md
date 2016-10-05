# Motivation

For many years, group chat on the internet was dominated by IRC, mailing lists and forums.

...more recently, Slack, which is largely inspired by IRC-clients but has a much better user on-boarding and day-to-day user experience, has been gaining many users inside of companies and online communities (such as Clojurians).


## Issue 1: Chatrooms are Counter-productive

 - incredibly easy to derail a conversation (usually with a related comment that starts a new tangent)
 - ...resulting in multiple simultaneous conversations in one room
   - ...making things hard to follow
   - ...and demanding your full attention (if you don't respond at the right time, that part of the conversation is gone, and answering now would derail)
   - resulting in a mix of many intertwined and never complete discussions

 - over time large rooms develop a unproductive culture, degrade to attention seeking (trolling, memes), and ostracize those hoping for productive conversations

 - fear-of-missing-out -> requiring constant checking


## Issue 2: Communication Infrastructure Should be Open

- not under risk of patron removing your rights
    (ex. Reactiflux community hitting Slack user limit and moving to Discord)

- extensibility: anyone can add features



## Braid Goals

 - encourage productive conversations
   - target audience: teams and online communities
   - allow for both real-time and async experiences
       (ie. don't demand the group's full attention just to keep the conversation on track)

Forums address some of these issues (but even threads still often have the tangent problem)... need to make threading easier, but this needs balance too (full threading results in fractal conversation, can also be difficult to follow)

Braid's major changes are:
 - thinking with tags instead of rooms (similar to how Gmail thinks with tags vs folders);
 - per-topic conversations, instead of per-room chatter (similar to how you have Facebook comments on each post)

 [[images/braid-concept.png]]

As a result...

  - you can have multiple simultaneous conversations related to a tag (vs. trying to keep track of multiple conversations in one room)

  - users can "mute" conversations (vs. having to deal with notifications for irrelevant messages in a room you want to stay in)

  - conversations can be resumed without context loss (vs. having to search for old messages)

  - no active conversations = your screen is empty (vs. old messages showing)

  - conversations can have multiple tags (vs. settling on the 'best' room)

  - conversations can change tags over time (vs. maintaining a conversation in an inappropriate room)


Martin has some great thoughts here: http://www.martinklepsch.org/chaf.html

More thoughts on threading here: https://hackpad.com/Chatroom-vs-....-bringing-threading-to-chat-8jJD3sxtRAf
