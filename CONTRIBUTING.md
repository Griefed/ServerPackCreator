## Contributing

If you want to contribute to SPC, please make sure your commits follow the conventional commit layout and use the types/categories/flags described here: [actions/auto-changelog](https://github.com/marketplace/actions/auto-changelog)

For completeness' sake:

```git
type(category): description [flag]
```

The `type` must be one of the followings:

* `breaking` (Breaking Changes)
* `build` (Build System / Dependencies)
* `ci` (Continuous Integration)
* `chore` (Chores)
* `docs` (Documentation Changes)
* `feat` (New Features)
* `fix` (Bug Fixes)
* `other` (Other Changes)
* `perf` (Performance Improvements)
* `refactor` (Refactors)
* `revert` (Reverts)
* `style` (Code Style Changes)
* `test` (Tests)

> If the `type` is not found in the list, it'll be considered as `other`.

The `category` is optional and can be anything of your choice.

The `flag` is optional (if provided, it must be surrounded in square brackets) and can be one of the followings:

* `ignore` (Omits the commit from the changelog)

> If `flag` is not found in the list, it'll be ignored.
