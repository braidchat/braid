# Getting Started

The following steps should get Braid running on your computer, ready for development.

If you're interested in getting Braid running in production, read: [Deploying Braid](./deploying.md)

To get Braid running locally, **you will need to have 2 or 3 terminal sessions open**:

 1. The Datomic Transactor (database), if you want the data to stay around
 2. The Braid REPL and Servers
 3. Figwheel (JS compiler and hot-reloader)


## 1. Datomic

Datomic is Braid's database. Datomic Free is fine for development, but Datomic Pro Starter is reccommended for production.

If you just want to test things out, by default Braid will use an in-memory Datomic database.
This means that when your repl closes, all data will be lost.

If you want the data to be persisted, follow the steps below to install Datomic:

1. Download Datomic Free 0.9.5201 from [https://my.datomic.com/downloads/free](https://my.datomic.com/downloads/free)

2. Unzip the download

To run Datomic:

1. In a terminal session, cd into the directory and run the transactor:

  ```bash
  cd ~/path/to/datomic-free-0.9.5201
  bin/transactor config/samples/free-transactor-template.properties
  ```

You will need to keep this process running during development. You can kill the process when you're not using it and restart it using the command above.


## 2. Braid Servers

The Braid Back-end spins up 3 processes:

  1. The API Server (which serves the HTTP and Websocket API, communicates with the database, etc.)
  2. The Desktop Client HTTP Server (which serves the HTML + JS assets for the desktop client)
  3. The Mobile Client HTTP Server (which serves the HTML + JS assets for the mobile client)

Before starting, you should have Java and [Leiningen](http://leiningen.org/) installed.

To get the server running:

1. Clone the braid repo (you may want to change the URL to your fork):

  ```bash
  git clone git@github.com:braidchat/braid.git
  ```

2. Go into the project directory:

  ```bash
  cd braid
  ```

2.5. (optional) If you're *not* using the in-memory version of Datomic, create a `profiles.clj` file with the following contents:

  ```clojure
  {:user {:env {:db-url "datomic:free://localhost:4334/braid"}}
  ```

3. Run the REPL:

  ```bash
  lein repl
  ```

4. Inside the REPL, start the servers:

  ```clojure
  (dev-main 5555)
  ```

This will start the desktop client server on 5555, the mobile client server on 5556, and the API server on port 5557.

5. Seed some data (first time only):

  ```clojure
  (require 'braid.server.seed)
  (braid.server.seed/seed!)
  ```

Don't navigate to the website just yet.


## 3. Figwheel

[Figwheel](https://github.com/bhauman/lein-figwheel) is used during development to compile Clojurescript to Javascript, and to hot-reload the code in the browser (ie. no need to refresh the browser when making changes).

In a new terminal session:

1. Navigate to the project folder:

  ```bash
  cd ~/path/to/braid
  ```

2. Run figwheel:

  ```bash
  lein figwheel desktop-dev
  ```

## ...and you're good!

Open `http://localhost:5555` in your browser:

```bash
open http://localhost:5555
```

Login with:

> username: `foo@example.com`
>
> password: `foofoofoo`

You should see a few messages and be able to reply.

If you edit a `.cljs` file in the repo, it should auto-update the page in the browser (no need for refreshing).

Note: currently in dev mode (the default profile), you cannot invite users or upload avatars. We're working on it.


## Issues and Extras


### Mobile Client

To work on the mobile client:

1. Run `figwheel` with the `mobile-dev` build:

  ```bash
  lein figwheel mobile-dev
  ```

2. Open `http://localhost:5556/` in your browser:

  ```bash
  open http://localhost:5556/
  ```


### A Better Clojurescript REPL

By default, the Clojurescript REPL that starts with figwheel doesn't support command history (up/down arrows, Ctrl-R, etc.) You can get this functionality back by launching figwheel with `rlwrap`

1. Install rlwrap:

  ```bash
  brew install rlwrap
  ```

2. Then, to start figwheel, use:

  ```bash
  rlwrap lein figwheel
  ```


### Using Emacs + CIDER instead of terminals

Emacs users who wish to have their repl sessions integrated with their development environment should follow these steps.

First,
[install CIDER](https://cider.readthedocs.io/en/latest/installation/).
Braid has the nREPL middleware CIDER depends on available under the `cider` profile in `project.clj`.
To use this profile in Emacs, you'll need to edit the `cider-lein-parameters` variable. There are two ways to do this:

* `M-x set-variable cider-lein-parameters`
* `C-h v cider-lein-parameters` and then click or hit enter on
  "customize" and set it there

In either case, set the value of the variable to be `with-profiles +cider repl :headless`

This should be sufficient to run a Clojure repl for server-side development.
To also integrate a ClojureScript repl for client-side development, follow the instructions from the Figwheel wiki [here](https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl#integration-with-emacscider).
Specifically, you'll need to add the following to your emacs config:

```emacs-lisp
;; ~/.emacs.el or ~/.emacs.d/init.el

;; somewhere after calling (require 'cider)
(setq cider-cljs-lein-repl
      "(do (require 'figwheel-sidecar.repl-api)
           (figwheel-sidecar.repl-api/start-figwheel!)
           (figwheel-sidecar.repl-api/cljs-repl))")
```

With that, you should be able to just run `M-x cider-jack-in-clojurescript` and emacs will launch both a Clojure and a ClojureScript repl configured for Braid development.


### Running Tests

#### Server Tests

```bash
lein test
```

Or, if you have quickie:

```bash
lein with-profile test quickie "chat.*"
```

#### Client Tests

Install [PhantomJS](http://phantomjs.org/) and ensure that the `phantomjs` binary is available from your path.
Once installed, you can run

``` bash
lein cljsbuild test once
```

to run the client-side tests once.
If you prefer to have tests run automatically as you make changes, then run

``` bash
lein cljsbuild test auto
```

### Permgen issues?

If you're experience the error: `java.lang.OutOfMemoryError: PermGen space`

Try:

1. Add the following to `project.clj` or `profiles.clj`:

  ```clojure
  :jvm-opts ["-XX:MaxPermSize=128m" "-XX:+UseConcMarkSweepGC" "-XX:+CMSClassUnloadingEnabled"]`
  ```

2. If you have lots of plugins in your lein :user profile (`~/.lein/profiles.clj`), remove some

3. Switch to Java 1.8


### Environment Variables

Various configuration options can be set via environment variables. Braid uses [Environ](https://github.com/weavejester/environ) to read environment variables.

An easy way to set variables during development is to create a `profiles.clj` file.

See [../profiles.sample.clj](../profiles.sample.clj) for sample profile options and instructions.


### Using Datomic Pro

To run Datomic Pro locally, you will need to:

  - sign up for an account with Datomic
  - download the pro version
  - unzip and run the transactor similarly as with the free version (but different template file)
  - add the following to the :chat lein profile (in your profiles.clj):

```clojure
  {:datomic-pro
    [:prod
    {:env {:db-url "datomic:dev://localhost:4333/braid"}}
     :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                      :creds :gpg
                                      :username "USERNAME-HERE"
                                      :password "PASSWORD-HERE"}}}]}
```

Henceforth, you will need to run lein with the profile:

  ```bash
  lein with-profile +datomic-pro repl
  ```


### Getting Started w/ Clojure

If you don't have Leiningen installed, then:

On Mac:

1. Install Homebrew by following the instructions at: [http://brew.sh/]()
2. Use Homebrew to install Leiningen:

  ```bash
  brew install leiningen
  ```

For other platforms, see: [https://github.com/technomancy/leiningen/wiki/Packaging]()


### Running in `:prod`

Start Redis running (used for cookies & invite tokens).  Braid currently assumes Redis is running on localhost on port 6379.
