# Code Organization

Braid's code is broken up into a few top-level namespaces most of which are "modules" (see below) .

```
braid.lib           "utility functions" ex. s3 upload
                       collections of pure stateless fns
                       nothing specific to braid, ie. could be useful in any project
                       freely accessible to modules
                       should only require from clojure, external libs, and other `braid.lib.*`
                         (ie. should not require anything for `braid.base`, `braid.chat`, `braid.some-module`)

braid.base          "framework"
                      contains stateful system "components"
                      bootstraps db, servers, module system, socket channels, client pages etc.
                      "buildings blocks of any generic web app"
                      `braid.base.api` ns exposes fns for downstream modules to use

braid.chat           a "module", implements most of core braid functionality
                        (users, groups, messages, etc.)
                        `braid.chat.api` ns exposes fns for downstream modules to use
                           new features may require exposing new hooks

braid.some-module    a "module", implementing some specific "optional" feature
                        effectively a 'micro-app' that implements some functionality
                          can be end-to-end: adding db state, socket messages, ui elements (ex. `stars`)
                          can be very minor: making use of a single hook (ex. `big-emoji`)
                        should be thought of as 'optional', or, 'behind a feature-flag'
                        can expose a `braid.some-module.api` ns with fns for other modules

braid.core           original code still needing to be re-organized
                        parts should be moved to `lib`, `base`, `chat`, or `some-module`
                        don't add new functionality here

```


## Braid Modules

Braid's "modules" are our way of managing the complexity of the growing code base,
by making use of [inversion-of-control](https://en.wikipedia.org/wiki/Inversion_of_control).
Other related concepts are [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) and [feature-flags](https://martinfowler.com/articles/feature-toggles.html).
Each "module" extends the base system, without the base system being aware of it
(think: Wordpress plugins, browser extensions, etc.). Braid "modules" have nothing to do with "java modules".

The `braid.base` provides modules the ability to hook into all the functionality needed to implement anything in a typical web app,
such as: defining db schema, db queries and transactions, jobs, http handlers, websocket message handlers (on client and server),
client-side events and subscriptions (re-frame), styles and views. As such, each module can be thought of as a 'micro-app' that implements a subset of Braid functionality.

Most new functionality can be written by "append-only" code in a new folder, with all related code to that feature in one place.
Sometimes, `braid.base` or `braid.chat` need to be modified to expose new functionality for a new module.

For example, the `braid.stars` module adds the ability to star threads. It uses functions exposed by `braid.base.api` to add to the db schema,
add websocket message handlers, client-side state, and client side event handlers. When writing it, we needed to expose a new function in the `braid.chat` module
to allow modules attach UI elements to the header part of a thread. The `braid.stars` module can be enabled/disabled at run-time
(currently, no UI for doing so, but it can be removed from `braid.modules/init`) and
all its functionality "disappears", while the rest of the system continues as is.

For some more background on the motivation behind this architecture, watch Raf's talk: [Composing Applications](https://www.youtube.com/watch?v=7HpI7d3-hpo).
The architecture is similar to [Polylith](https://polylith.gitbook.io/polylith/); and in some ways similar to [Arachnae](https://github.com/arachne-framework);
but the implementation is it's own thing because these projects didn't exist when we started Braid.


## Dev Tips:
  - look to `braid.base.api`, `braid.chat.api` to see what functionality is available
  - look at some existing modules (ex. `braid.stars`) to see how they get things done
  - when adding new features, if possible, make it a new module
      ie. use the fns already existing `braid.base.api`, `braid.chat.api` and `braid.other-modules.api`
        (possibly, may need to add a new hook to base, chat, or other-module)





