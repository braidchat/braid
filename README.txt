# starting

; start datomic db elsewhere

lein with-profile lp repl

(chat.server.db/init!) ; first time only

(chat.server.seed/seed!) ; optional

(chat.server.handler/start-server! 5555)
(chat.server.sync/start-router!)


# running tests

lein with-profile test,lp quickie "chat.*"
