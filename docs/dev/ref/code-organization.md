# Code Organization

Braid is broken up into modules.

## Module Structure

Each plugin resides in a namespace under `braid`, e.g. a plugin called `braid.coolplugin` would reside in `src/braid/coolplugin/`.
The top-level folder of the module contains a file name `module.edn` (e.g. `src/braid/coolplugin/module.edn`).
The `module.edn` contains a map with the following structure (note that all keys are optional):

 - `:name` - A symbol given the name of the module
 - `:doc`  - A string documenting the purpose of the module
 - `:provides` - A map which can contain the keys `:clj` and `:cljs`, which themselves will indicate the extension points the module provides (see below).
 - `:extends` - A map which can contain the keys `:clj` and `:cljs`, which will contain maps indicating the extension points this plugin extends (see below).

### Providing Extension Points

### Extending Extension Points
