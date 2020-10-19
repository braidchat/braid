# Getting Started

The following steps should get Braid running on your computer, ready for development.

If you're interested in getting Braid running in production, read: [Deploying Braid](../drafts/deploying-to-production.md)

## 0. Prep

Before running Braid, you'll need to have Java and Leiningen installed.

1. Check if you have java installed by running `java -version` from your commandline. It should be at least version 1.8.0. If not, install openjdk or Oracle Java (the exact procedure depends on your OS, see Google).

2. Check if you have leiningen installed by running `lein --help` from your commandline. If it's not installed, see the [Leiningen website](http://leiningen.org/) for instructions.

3. Clone the braid repo (you may want to change the URL to your fork):

  ```bash
  git clone git@github.com:braidchat/braid.git
  ```

4. Go into the project directory:

  ```bash
  cd braid
  ```


## 1. Braid REPL & Servers

From the project directory...

1. Run the REPL:

  ```bash
  lein repl
  ```

The repl starts you off in `braid.dev.core`, which is a dev-only namespace with some utility functions. The source is in: `dev-src/braid/dev/core.clj`

The main app entry point is `braid.core` with source under: `src/braid/core.clj`

2. Inside the REPL, start the Braid system:

  ```clojure
  (start! 5555)
  ```

This will start all the various Braid components, including 3 web servers:

| Server             | Port | Description                                                     |
| ------------------ | ---- | --------------------------------------------------------------- |
| desktop web client | 5555 | HTML, JS, CSS assets for desktop web client                     |
| mobile web client  | 5556 | HTML, JS, CSS assets for mobile web client                      |
| api                | 5557 | HTTP and Websocket API, communicates w/ db, etc.                |
| figwheel           | 3559 | cljs->js compiler and live-code reloader, do not visit directly |


3. Seed some data:

  ```clojure
  (seed!)
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

In a private window, you can login as another user:

> username: `bar@example.com`
>
> password: `barbarbar`

If you edit a `.cljs` file in the repo, it should auto-update the page in the browser (no need for refreshing). Note: when developing, you should always have the Chrome/Firefox inspector, with "Disable Cache" on (under the Network Tab).


## Datomic

By default, Braid uses Datomic's in-memory database, which requires no set-up, but, it requires re-seeding every time you restart the REPL.

To have data survive a REPL restart, you'll need to persist it to disk by installing Datomic.

To install "Datomic Free":

1. Download Datomic Free 0.9.5201 from [https://my.datomic.com/downloads/free](https://my.datomic.com/downloads/free)

2. Unzip the download

To run Datomic:

1. In a terminal session, cd into the directory and run the transactor:

  ```bash
  cd ~/path/to/datomic-free-0.9.5201
  bin/transactor config/samples/free-transactor-template.properties
  ```

You will need to keep this process running during development. You can kill the process when you're not using it and restart it using the command above.

In your Braid project, you'll need to create a `profiles.clj` with the following (and restart the REPL to pick up the changes).

  ```clojure
  ;; deprecated method
  {:user {:env {:db-url "datomic:free://localhost:4334/braid"}}
  ;; OR
  ;; new method, requires you to start the repl like `lein with-profile +dev repl`
  ;; instead of just `lein repl`
  {:dev {:env {:db-url "datomic:free://localhost:4334/braid"}}
  ```

In production, we recommend "Datomic Starter" instead (instructions [here](../drafts/deploying-to-production.md)).


## Extras


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

### Using Emacs + CIDER instead of terminals

Emacs users who wish to have their repl sessions integrated with their development environment should follow these steps.

First, [install CIDER](https://cider.readthedocs.io/en/latest/installation/).
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

Install [PhantomJS](http://phantomjs.org/) and ensure that the `phantomjs` binary is available from your path.  Once installed, you can run:

``` bash
lein cljsbuild test once
```

to run the client-side tests once.
If you prefer to have tests run automatically as you make changes, then run

``` bash
lein cljsbuild test auto
```

### Permgen issues?

If you're experience the error: `java.lang.OutOfMemoryError: PermGen space`, try the following:

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

### Getting Started w/ Clojure

If you don't have Leiningen installed, then:

On Mac:

1. Install Homebrew by following the instructions at: [http://brew.sh/]()
2. Use Homebrew to install Leiningen:

  ```bash
  brew install leiningen
  ```

For other platforms, see: [https://github.com/technomancy/leiningen/wiki/Packaging]()

