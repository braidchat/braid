# starting

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

