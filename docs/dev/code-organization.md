# Code Organization

Braid's code is broken up into a few top-level namespaces and several "modules".

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


Tips:
  - look to `braid.base.api`, `braid.chat.api` to see what functionality is available
  - look at some existing modules (ex. `braid.stars`) to see how they get things done
  - when adding new features, if possible, make it a new module
      ie. use the fns already existing `braid.base.api`, `braid.chat.api` and `braid.other-modules.api`
        (possibly, may need to add a new hook to base, chat, or other-module)





