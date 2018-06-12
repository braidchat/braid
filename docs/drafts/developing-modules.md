# Developing Braid Modules

## Module Spec

- a module is self-contained in a folder directly under `braid`, ie. `src/braid/widget`

`braid.widget.core`

- a module must have a `braid.widget.core` namespace
- `braid.widget.core` must be a `cljc` file
- `braid.widget.core` must have a namespace docstring explaining the purpose of the module
- `braid.widget.core` must have a non-private `init!` function
- `braid.widget.core` can contain implementation details or require functions or values from other namespaces
- all other functions in `braid.widget.core` (other than `init!`) should be private

`braid.widget.api`

- a module can optionally expose functionality to other modules via a `braid.widget.api` namespace
- `braid.widget.api` must be a `cljc` file
- `braid.widget.api` should only contain functions (or other values) intended for use by other modules (all "private" values should be in other namespaces under `braid.widget.*`)
- all public functions should have docstrings
- all public functions should validate their inputs (for example using `:pre` conditions and spec)



## Development Notes

- new modules need to be added to `braid.core.modules`
- most "new functionality" should be implemented by creating new modules rather than changing exist modules
- you may need to expose functionality in an existing module to achieve what you need
- you probably want to make use of `braid.core.hooks`


## Notes for Future

 - a way for each module to declare library dependencies
 - a way for each module to declare tests




