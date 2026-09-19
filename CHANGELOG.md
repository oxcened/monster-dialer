# Changelog

All notable changes to Monster Dialer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Retro contact and call experience improvements.
- Character roster and default-character selection improvements.

## [0.6.0] - 2026-09-06

### Added

- Global contact-character defaults for both trainers and monsters, including a
  default character or a configurable randomizer pool for contacts without
  their own customization.
- Per-contact randomizer pools, so individual contacts can draw from a
  different set of eligible characters.
- A fast-scroll control in character selectors for quickly moving through large
  character collections.
- A QR-code sheet for sharing an online profile.

### Changed

- Contact character settings now clearly distinguish between global defaults,
  a custom contact assignment, and Randomize mode.
- The contact-character toolbox and settings screens were reorganized around
  separate trainer and monster controls, with clearer customization guidance.
- Randomizer pools now support selecting all available characters, selecting a
  subset, resetting to the full pool, and safely discarding an empty unsaved
  pool.
- Character selection headers and controls are more compact, and selection
  behavior is consistent across list and grid layouts.
- Online profile actions were streamlined: enabling a profile, signing in with
  Google, QR-code sharing, regeneration, sign-out, and deletion are grouped
  into clearer actions and menus.
- The shared OpenDialer implementation is now pinned as a git submodule,
  simplifying repository setup and keeping application and dialer revisions
  synchronized.
- Published-release Discord notifications now include the release notes, APK
  download, and a development-support link; only major releases mention
  `@everyone`.

### Fixed

- Contact-specific character settings now persist correctly and can inherit
  global defaults when no override is configured.
- Contact randomizer pools preserve configured entries, honor built-in
  characters, and remain aligned when editing or scrolling through selections.
- Restored contact characters in the character toolbox and stabilized contact
  settings when switching between contacts or character types.
- Corrected automatic scrolling and selected-state handling when a character
  is the built-in default or when Randomize mode is active.

## [0.5.0] - 2026-09-04

### Added

- A redesigned character profile home screen with your trainer, active monster,
  collection progress, radiant discoveries, battle count, and character tools.
- Player roster management for up to six monsters, including adding, removing,
  selecting, and drag-and-drop reordering.
- A local Battle Journal for reviewing recent battles, radiant discoveries, and
  repeat radiant encounters, with filters, saved artwork, sharing, and a clear
  history action.
- Randomized contact characters, allowing contacts to receive a different
  eligible opponent on each call.
- Optional Online Profiles that let friends view the trainer and active monster
  used in calls through a shareable link.
- Google account sign-in for creating and managing an Online Profile, including
  profile recovery across devices.
- QR-code sharing and contact linking for Online Profiles.
- In-app Character, Radiants, roster, Online Profile, and sharing guides.

### Changed

- The character experience is organized around a profile, roster, collection,
  contact customization, and a compact character toolbox.
- The active monster is now a real selectable roster entity, with its variant,
  level, and artwork reflected consistently throughout the profile and battles.
- Built-in monsters participate in the same character-assignment and battle
  flows as other supported characters.
- Radiant discoveries now update profile progress and are retained in the Battle
  Journal with journal-owned artwork thumbnails.
- Online Profile encounters are fetched and cached for battles without making
  Firebase responsible for placing, answering, routing, or controlling calls.
- Online Profile sharing stores only the public profile data remotely; contact
  links remain local to the device, and sharing links can be regenerated or
  permanently deleted.
- Online Profile backend access now uses Google authentication, Firestore,
  Cloud Storage, ownership checks, and Firebase App Check.
- Character packs, character creation, sharing, and import actions now use the
  redesigned profile toolbox and clearer contextual help.
- Added custom dialer and character-pack controls, plus an icon-designer credit.

### Fixed

- Preserved the selected active monster and roster order while editing the
  player character settings.
- Guarded roster replacement and full-roster interactions so invalid changes do
  not remove or overwrite the wrong monster.
- Corrected active radiant and regular variant display in profile and battle
  views.
- Prevented contacts without usable phone numbers from being selected for
  character customization or Online Profile linking.
- Hardened Online Profile sharing, profile-link handling, and Firebase-backed
  cleanup when remote data is unavailable or invalid.
- Restored contextual help and added missing localized strings across the new
  character and Online Profile flows.

## [0.4.0] - 2026-09-01

### Added

- A character hub that brings together player characters, contact characters,
  character packs, imports, and customization help.
- Simplified creation and editing for custom trainers and monsters, including
  front and back sprites, monster levels, maximum HP, and optional radiant
  variants.
- Sharing and importing for individual `.monstercharacter` files and
  `.monsterpack` collections.
- In-app character-pack creation with pack name, version, creator, license, and
  character metadata.
- Radiant monster variants that can be discovered during wild battles, then
  unlocked, assigned, and shared as discovery cards.
- Persistent selection of the last-used character tab.
- Character and pack metadata previews, copyable metadata, and clearer import,
  enable, disable, and removal actions.
- Monster-specific conference and held-call artwork for in-call controls.
- A Character Guide explaining custom characters, sprites, packs, radiant
  variants, and player/contact assignments.

### Changed

- Character collections are grouped by source, with separate sections for
  built-in characters, custom characters, and imported packs.
- Character creation now validates sprites according to their assignment:
  front sprites are needed for contact characters and back sprites for the
  player character. Trainers show only the settings that apply to them.
- Monster cards now identify Regular, Radiant, and Locked variants clearly,
  while radiant settings and monster-only stats are kept in an expandable
  Advanced section.
- Character packs can contain up to 200 characters and show their installed and
  enabled state in settings.
- Layout controls, add actions, empty states, and navigation now adapt to the
  available character content; list view is used when a grid has no meaningful
  options.
- In-call secondary-call information is presented as an overlay so it does not
  displace the active battle, and held calls retain their correct artwork.

### Fixed

- Custom character lists update immediately after creating, editing, deleting,
  or importing a character.
- Character and pack assignments are cleared safely when an in-use character or
  pack is removed or disabled, with a warning before the destructive action.
- Shared-character import rejects invalid files and routes character-pack files
  through the correct import flow.
- Character selection auto-scrolls correctly across grouped roster sections and
  after navigation state restoration.
- Image selections are preserved when the image picker is cancelled, and
  missing battle sprites fall back safely instead of breaking the encounter.
- Radiant encounters persist correctly across activity recreation and concurrent
  calls, and radiant variants are unlocked and notified at the right time.
- Dialogue rendering no longer fails when the available measurement width is
  constrained.

## [0.3.0] - 2026-08-29

### Added

- Separate Trainer and Monster tabs in player and contact character settings.
- List and grid layouts for character collections, plus a jump-to-selected
  control for navigating large collections.
- GitHub release update checks so the app can identify a newer published
  version.
- A themed pixel-art icon set across the dialer, call controls, contacts,
  favorites, voicemail, and character flows.
- The Pixel Operator typeface and a Monster Dialer-branded visual theme.

### Changed

- The app architecture now follows the OpenDialer Screen → ViewModel →
  Repository pattern, with focused repositories and read-only UI state flows.
- Character, contact, and pack features were reorganized into dedicated feature
  areas with shared selector components.
- Character selectors now render lazily and preserve navigation state while
  switching tabs or returning to settings.
- The app shell, typography, translations, and branded resources were aligned
  with the OpenDialer foundation.

### Fixed

- Disabled character packs remain visible in the appropriate preview state
  instead of disappearing unexpectedly.
- Character-pack empty states are centered and aligned with the surrounding
  settings UI.
- Character selection navigation and the selected-character position remain
  stable while switching layouts and tabs.

## [0.1.3] - 2026-08-27

### Fixed

- Preserved PrettyTime locale bundles in release builds.

## [0.1.2] - 2026-08-27

### Changed

- Enabled R8 optimization for release builds.

## [0.1.1] - 2026-08-27

### Changed

- Updated the pinned OpenDialer revision.

### Fixed

- Removed unnecessary locale overrides.
- Repaired Discord release payload handling.

## [0.1.0] - 2026-08-27

### Added

- The first published Monster Dialer release.

[unreleased]: https://github.com/oxcened/monster-dialer/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/oxcened/monster-dialer/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/oxcened/monster-dialer/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/oxcened/monster-dialer/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/oxcened/monster-dialer/compare/v0.1.3...v0.3.0
[0.1.3]: https://github.com/oxcened/monster-dialer/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/oxcened/monster-dialer/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/oxcened/monster-dialer/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/oxcened/monster-dialer/releases/tag/v0.1.0
