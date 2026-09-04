# MonsterDialer localization agent instructions

You maintain Android string resources for MonsterDialer. Translate new, missing, or revised
user-facing strings directly from the English source and its Android `comment` context. Do not
use Google Translate, a machine-translation API, or a translation website. Generate the wording
yourself, then edit it for natural, compact UI language.

## Scope and source of truth

- The source resource file is `app/src/main/res/values/strings.xml`.
- Supported localized files are `app/src/main/res/values-{ar,da,de,es,fr,it,pt,ro,zh}/strings.xml`.
- Never translate resources marked `translatable="false"`; Android resolves those from the base
  resource file. Do not add localized copies of URLs, app names, font credits, version numbers,
  file extensions, or other non-translatable metadata.
- Preserve user-authored translations and unrelated working-tree changes. Do not rewrite an
  entire resource file merely to add a feature batch.
- Keep MonsterDialer, Firebase, identifiers, file extensions, URLs, and format placeholders
  unchanged unless the English source explicitly says otherwise.

## Translation workflow

1. Identify the feature cohort in the English source. Translate the entire cohort, not isolated
   keys, so repeated product terms and dialog language stay consistent.
2. Read each string's `comment`, its neighboring source strings, and the screen that uses it when
   the intended meaning is unclear.
3. Establish a small, feature-specific terminology decision before translating. Reuse the same
   localized term for concepts such as profile, sharing link, character, team, trainer, monster,
   and radiant throughout that cohort.
4. Generate every target-language translation yourself. Prefer the existing locale's tone and
   terminology. Keep buttons, navigation items, and accessibility labels as short as clarity
   permits.
5. Make a separate editing pass for each language. Check grammar, punctuation, politeness level,
   grammatical gender, plural agreement, and natural phrasing. Check Arabic especially for
   right-to-left readability; do not insert directionality controls unless technically required.
6. Add only the missing or revised strings to the appropriate localized files. Preserve XML
   formatting and escape XML characters where required.

## Android resource rules

- Preserve every format placeholder exactly, including its index and conversion type: `%1$s`,
  `%2$d`, `%d`, and `%%`.
- Preserve escaped newlines (`\\n`), markup, and any intentional whitespace from the source.
- Do not change resource names, remove strings, or duplicate keys.
- Add plural resources with the locale-appropriate Android quantity categories when a new plural
  is introduced. In Compose, plural resources must be read with `pluralStringResource`, not
  `stringResource`.
- Do not introduce trademarked monster-franchise names. Use the project’s generic terminology.

## Required audit

Before reporting completion, compare each locale against the English source while excluding
`translatable="false"` strings. Confirm that every localized translatable string exists exactly
once and that all placeholders match the source.

Run this audit from the repository root:

```sh
ruby -r rexml/document -e '
base = REXML::Document.new(File.read("app/src/main/res/values/strings.xml")); source = {}
base.elements.each("resources/string") { |e| source[e.attributes["name"]] = e unless e.attributes["translatable"] == "false" }
failed = false
Dir["app/src/main/res/values-*/strings.xml"].sort.each do |path|
  doc = REXML::Document.new(File.read(path)); entries = {}; duplicates = []
  doc.elements.each("resources/string") { |e| key = e.attributes["name"]; duplicates << key if entries[key]; entries[key] = e }
  missing = source.keys - entries.keys
  mismatch = source.keys.select { |key| entries[key] && source[key].text.to_s.scan(/%(?:\\d+\\$)?[ds]/).sort != entries[key].text.to_s.scan(/%(?:\\d+\\$)?[ds]/).sort }
  puts "#{File.basename(File.dirname(path))}: missing=#{missing.length}, duplicates=#{duplicates.length}, placeholder_mismatches=#{mismatch.length}"
  failed ||= missing.any? || duplicates.any? || mismatch.any?
end
exit(failed ? 1 : 0)'
```

Then run `git diff --check` and `./gradlew :app:lint`. If lint fails, report the exact failures
and whether they are caused by the localization change; do not change unrelated code merely to
make lint green.

## Change and review policy

- Keep all languages for one feature in one focused pull request when requested.
- Use a Conventional Commit if committing, for example `feat: localize online profiles`.
- Never commit directly to `main`. Do not create a commit or pull request unless the user asks.
- In the handoff, report the feature translated, languages changed, audit result, lint result, and
  any remaining gaps or review risks.
