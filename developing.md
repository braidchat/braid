## starting

Create a `profiles.clj` that looks something like
```clojure
{:chat
 {:env {:rafal-password "some test password"
        :james-password "some test password"
        :mailgun-domain "braid.mysite.com"
        :mailgun-password "my_mailgun_key"
        :site-url "http://localhost:5555"
        :hmac-secret "foobar"
        :aws-domain "braid.mysite.com"
        :aws-access-key "my_aws_key"
        :aws-secret-key "my_aws_secrete"
        :db-url "datomic:free://localhost:4334/chat-dev"
        }}}
```
(quick note until we clean this up:
   you can probably get by with only db-url and hmac-secret)

```bash
# start datomic db elsewhere
# then:
lein with-profile +chat repl
```

```clojure
(chat.server.db/init!) ; first time only

(chat.server.seed/seed!) ; optional

(chat.server.handler/start-server! 5555)
(chat.server.sync/start-router!)
```

# compiling js + figwheel

rlwrap lein run -m clojure.main script/figwheel.clj

# compiling css

lein lesscss auto

# running tests

`lein with-profile test quickie "chat.*"`



# permgen issues?

if you're having the following error:

`java.lang.OutOfMemoryError: PermGen space`

try:

1: add the following to project.clj or profiles
`:jvm-opts ["-XX:MaxPermSize=128m" "-XX:+UseConcMarkSweepGC" "-XX:+CMSClassUnloadingEnabled"]`

2: if you have lots of plugins in your lein user profile (`~/.lein/profiles.clj`), remove them

3: switch to java 1.8

