# Dev

To get Braid running locally, you will need to have 3 terminal sessions open:
1 - Datomic transactor
2 - Braid REPL and server
3 - Figwheel (JS + CSS compile and Hot Reload)

## datomic

Download Datomic Free 0.9.5201 from [https://my.datomic.com/downloads/free](https://my.datomic.com/downloads/free)

Unzip the download

In a terminal session, cd into the directory:
```bash
cd datomic-free-0.9.5201
```

Run the transactor:
```bash
bin/transactor config/samples/free-transactor-template.properties
```

Datomic Free runs in memory, so if you quit the transactor process, all data will be lost (which is OK in dev). If you want to avoid this, you can use Datomic Pro (see notes below).

## braid server

In a seperate terminal session:

Clone the braid repo (you may want to change the URL to your fork):
```bash
git clone git@github.com:braidchat/braid.git
```

then:
```bash
lein repl
```

Inside the REPL:

Start things running
```clojure
(dev-main 5555) ; start main server on port 5555
```

Seed some data (first time only):
```clojure
(require 'braid.server.seed)
(braid.server.seed/seed!)
```

Don't navigate to the website just yet.

## Figwheel - JS compiling and hot-reload

In a seperate terminal session:

```bash
lein figwheel desktop-dev
```

## ...and you're good!

Open your browser to: `http://localhost:5555`

Login with `foo@example.com` `foo`

You should see a few messages and be able to reply.

If you edit a cljs file in the repo, it should auto-update the page in the browser (no need for refreshing).

Note: currently in dev mode (the default profile), you cannot invite users or upload avatars. We're working on it.

# Issues and Extras

## mobile dev

for working on the mobile client:

```bash
lein figwheel mobile-dev
```

open:
`http://localhost:3449/mobile.html`


## better clojurescript repl

Install rlwrap
```bash
brew install rlwrap
```

Then, to start figwheel, use:
```bash
rlwrap lein run -m clojure.main script/figwheel.clj
```

## running tests

`lein test`

or if you have quickie:
`lein with-profile test quickie "chat.*"`

## permgen issues?

if you're having the error: `java.lang.OutOfMemoryError: PermGen space`

try:

1: add the following to project.clj or profiles.clj
`:jvm-opts ["-XX:MaxPermSize=128m" "-XX:+UseConcMarkSweepGC" "-XX:+CMSClassUnloadingEnabled"]`

2: if you have lots of plugins in your lein :user profile (`~/.lein/profiles.clj`), remove them

3: switch to java 1.8

## Using Datomic Pro

To run Datomic Pro locally, you will need to:
  - sign up for an account with Datomic
  - download the pro version
  - unzip and run the transactor similarly as with the free version (but different template file)
  - add the following to the :chat lein profile (in your profiles.clj):

```clojure
  {:chat
    [:prod
    {:env {:db-url "datomic:dev://localhost:4333/braid"}}
     :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                      :creds :gpg
                                      :username "USERNAME-HERE"
                                      :password "PASSWORD-HERE"}}}]}
```

Henceforth, you will need to run lein with: `lein with-profile +chat repl`


## Other Profile Options

In the project folder, create a profile
```clojure
{:chat
 {:env {
        ; for invite emails:
        :mailgun-domain "braid.mysite.com"
        :mailgun-password "my_mailgun_key"
        :site-url "http://localhost:5555"
        :hmac-secret "foobar"
        ; for image uploads
        :aws-domain "braid.mysite.com"
        :aws-access-key "my_aws_key"
        :aws-secret-key "my_aws_secret"
        :s3-upload-key "key-for-uploading"
        :s3-upload-secret "secret-for-uploading"
        ; for link info extraction
        :embedly-key "embedly api key"
        ; for github login
        :github-client-id "..."
        :github-client-secret "..."
        }}}
```

## Getting Started w/ Clojure

If you don't have leiningen installed, then:

On Mac:
  Install brew by following the insturctions at: [http://brew.sh/]()
  Then: `brew install leiningen`

For other platforms, see: [https://github.com/technomancy/leiningen/wiki/Packaging]()

## Running in :prod

Start Redis running (used for cookies & invite tokens).  Braid currently assumes
Redis is running on localhost on port 6379.
