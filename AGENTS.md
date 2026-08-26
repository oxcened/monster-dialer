# Commit conventions

Always use Conventional Commits for commit messages.

# Naming conventions

Do not use trademarked monster names (e.g., from the Pokemon franchise) in the codebase, including tests, documentation, and assets. Use generic monster names instead (e.g., Mossling, Fernfox).

# Localization

Never hard-code user-visible strings in Kotlin, XML layouts, or Compose UI. Put them in Android string resources and use formatted or plural resources where appropriate.
In Compose, load `<string>` resources with `stringResource` and `<plurals>` resources with `pluralStringResource`; do not use `stringResource` with a plural resource ID.
When translating UI labels, account for the available space: keep compact controls such as buttons and navigation labels as short as the target language allows, without sacrificing clarity.
