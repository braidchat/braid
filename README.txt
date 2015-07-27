# starting

; start datomic db elsewhere

lein with-profile lp repl

(chat.server.db/init!) ; first time only

(chat.server.seed/seed!) ; optional

(chat.server.handler/start-server! 5555)
(chat.server.sync/start-router!)


# running tests

lein with-profile test,lp quickie "chat.*"


# deploying

`./deploy.sh`

then ssh to chat.leanpixel.com and

`sudo supervisorctl restart chat`


# production notes

nrepl is started on port 3081.  To connect, forward the port when ssh'ing in, e.g.

`ssh -L 3081:localhost:3081 chat.leanpixel.com`

then you can `lein repl :connect 3081` locally.

App log files are at `/www/deploys/chat/chat-server.log` and `/www/deploys/chat/chat-server.err.log` (stdout & stderr, respectively)

Datomic logs are at `/home/james/datomic.log` and `/home/james/datomic.err.log` (bad location, I should update this)

Nginx logs are at `/var/log/nginx/chat.access.log` and `/var/log/nginx/chat.error.log`
