# Developing Braid Modules

## Module Spec

- a module is self-contained in a folder directly under `braid`, ie. `src/braid/widget`
- a module must have a `braid.widget.core` namespace
- `braid.widget.core` must be a `cljc` file
- `braid.widget.core` must have an `init!` function
- `braid.widget.core` should only contain `init!` and functions (or other values) that can be publically accessed by other modules (all "private" values should be in other namespaces under `braid.widget.*`)
- `braid.widget.core` should have a namespace docstring explaining the purpose of the module
- all public functions should have docstrings
- all public functions should validate their inputs (for example using `:pre` conditions and spec)
- if requiring namespaces from other Braid modules, only the `core` namespaces should be required


## Development Notes

- new modules need to be added to `braid.core.modules`
- most "new functionality" should be implemented by creating new modules rather than changing exist modules
- you may need to expose functionality in an existing module to achieve what you need
- you probably want to make use of `braid.core.hooks`


## Notes for Future

 - a way for each module to declare library dependencies
 - a way for each module to declare tests




