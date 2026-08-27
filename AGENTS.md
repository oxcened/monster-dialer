# Commit conventions

Always use Conventional Commits for commit messages.

# Naming conventions

Do not use trademarked monster names (e.g., from the Pokemon franchise) in the codebase, including tests, documentation, and assets. Use generic monster names instead (e.g., Mossling, Fernfox).

# Localization

Never hard-code user-visible strings in Kotlin, XML layouts, or Compose UI. Put them in Android string resources and use formatted or plural resources where appropriate.
In Compose, load `<string>` resources with `stringResource` and `<plurals>` resources with `pluralStringResource`; do not use `stringResource` with a plural resource ID.
Do not call `LocalContext.current.getString()` or read `LocalContext.current.resources` from a composable. Use `stringResource`/`pluralStringResource`, or `LocalResources.current` when a resource must be resolved dynamically, so configuration changes invalidate the composition.
When translating UI labels, account for the available space: keep compact controls such as buttons and navigation labels as short as the target language allows, without sacrificing clarity.

# Git workflow

- GitHub release changelog generation relies on merge commits. Make every change through a pull request and merge it into `main`; never commit directly to `main`.
- Keep each pull request focused on one topic.
