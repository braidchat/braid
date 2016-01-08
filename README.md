# Braid

Braid is an open-source group chat application for teams and communities designed to promote productive conversations.

It is written in clojure(script) and developed by the clojure community.

Read [the Background](https://github.com/braidchat/braid/wiki) and [Motivation](https://github.com/braidchat/braid/wiki/Motivation).


Circumstances led us to open-source this sooner than we were planning, so there are a lot of rough edges. Please be patient.

In the short-term, we will be focusing on the ‘get-it-running-on-your-machine’ experience. In the meantime, you can try to decipher our setup notes here:

https://github.com/braidchat/braid/blob/master/deploying.md


Conversations are current happening on the #clj-chat-project channel at clojurians.slack.com (until we start dogfooding, of course!)


## starting

Create a `profiles.clj` that looks something like
```clojure
{:chat
 {:env {:rafal-password "some test password"
        :james-password "some test password"
        :mailgun-password "my_mailgun_key"
        :site-url "http://localhost:5555"
        :hmac-secret "foobar"
        :aws-access-key "my_aws_key"
        :aws-secret-key "my_aws_secrete"
        :db-url "datomic:dev://localhost:4334/chat-dev"
        }}}
```

```bash
# start datomic db elsewhere
lein with-profile +chat repl
```

```clojure
(chat.server.db/init!) ; first time only

(chat.server.seed/seed!) ; optional

(chat.server.handler/start-server! 5555)
(chat.server.sync/start-router!)
```

# compiling js

rlwrap lein run -m clojure.main script/figwheel.clj

# compiling css

lein lesscss auto

# running tests

`lein with-profile test,lp quickie "chat.*"`

