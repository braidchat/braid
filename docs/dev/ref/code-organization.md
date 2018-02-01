# Code Organization

Braid is broken up into modules.

For an example of a module definition file, see `src/braid/core/module.edn`.

## Module Structure

Each plugin resides in a namespace under `braid`, e.g. a plugin called `braid.coolplugin` would reside in `src/braid/coolplugin/`.
The top-level folder of the module contains a file name `module.edn` (e.g. `src/braid/coolplugin/module.edn`).
The `module.edn` contains a map with the following structure (note that all keys are optional):

 - `:name` - A symbol given the name of the module
 - `:doc`  - A string documenting the purpose of the module
 - `:provides` - A map which can contain the keys `:clj` and `:cljs`, which themselves will indicate the extension points the module provides (see below).
 - `:extends` - A map which can contain the keys `:clj` and `:cljs`, which will contain maps indicating the extension points this plugin extends (see below).

### Providing Extension Points

A module can provide extension points by listing functions under the `:provides` key of the module.edn map, under either the `:clj` or `:cljs` key, depending on which runtime the function is meant to extend from.

The maps under `:clj`/`:cljs` indicate extension points.
The keys should be keywords and will be the names that `:extends` maps will use to extend said extension points.
The values will be maps with two keys:
`:doc` is a docstring describing what the extension point is for and `:fn` is a namespaced symbol indicating the function that will be called to register point (see "Extending..." below).


e.g.

```clj
{...
 :provides
 {:clj
  {:register-thing {:fn braid.coolplugin.core/register-thing!
                    :doc "Adds a new thing to coolplugins registry"}}}}
```

### Extending Extension Points

To extend an extension point that another module provides. modules indicate in their module.edn, under the `:extends` key, which points they wish to extend.
The value under `:extends` will be a map that map have the keys `:clj` or `:cljs` (like `:provides`).
The map under the `:clj`/`:cljs` key indicates which extension points to call with what arguments.
The values must correspond to keys in the `:provides` map of some module (with the same target (i.e. `:clj` or `:cljs`)) and the key can either be a namespaced symbol, in which case the value of that symbol will be passed as the argument to the corresponding `provide`-ed `:fn`, or a sequence of namespaced symbols, which will be treated like calling the `:fn` on each of the arguments.

e.g.

```clj
{...
 :extends
 {:clj
  {:register-thing braid.otherplugin.bloop/some-argument
   ;; equivalent to
   ;:register-thing [braid.otherplugin.bloop/some-argument]

   ;; will call the :fn that was provided for the :register-other-thing
   ;; key twice, once with `foo` as the argument, once with `bar`.
   :register-other-thing [braid.otherplugin.blaap/foo
                          braid.otherplugin.blaap/bar]
  }}
}
```
