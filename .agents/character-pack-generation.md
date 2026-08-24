# MonsterDialer character-pack generation instructions

You are creating a self-contained, importable MonsterDialer character pack. Follow every
requirement below exactly. Do not add unrequested fields, files, network references, or code.

## Input to collect

Use the supplied pack ID, name, version, creator, licence statement, and character briefs.
Each character brief must state its name, visual direction, whether it is a `trainer` or
`monster`, and whether it is assignable to `contact`, `player`, or both. If a required brief is
absent, ask for it before generating art.

Only create artwork that is original or that the user has explicitly confirmed they own or are
licensed to use. Do not use protected franchise characters, names, logos, or copied assets.

## Deliverables

Build the following staging contents in `test-packs/<pack-file-name>/`:

1. `manifest.json`, encoded as UTF-8.
2. A transparent front-view PNG for every trainer and monster.
3. A distinct transparent rear-view PNG for every character assignable to `player`.
   The rear image must be a true back view—not a front image, mirror, or three-quarter pose.
4. Optional `.ogg` call sounds only when explicitly requested.
5. `test-packs/<pack-file-name>.zip`, containing the manifest and all referenced media.

Use original artwork with no text, watermark, logo, scenery, or opaque background unless the
user explicitly asks otherwise. Save generated project assets into the workspace; never leave
referenced assets only in a generated-images or temporary directory.

## Manifest contract

Write exactly this JSON shape. Omit optional properties entirely when no value is supplied.
Unknown properties are invalid.

```json
{
  "formatVersion": 2,
  "id": "com.example.forest-friends",
  "name": "Forest Friends",
  "version": "1.0.0",
  "license": "Original artwork, used with permission",
  "creator": "Example Studio",
  "characters": [
    {
      "id": "mossling",
      "name": "Mossling",
      "type": "monster",
      "level": 12,
      "maxHp": 45,
      "assignableTo": ["contact", "player"],
      "frontImage": "art/mossling.png",
      "backImage": "art/mossling-back.png",
      "callSound": "audio/mossling.ogg"
    }
  ]
}
```

`creator`, `backImage`, `callSound`, `level`, and `maxHp` are optional. `frontImage` is required.

| Field | Constraint |
| --- | --- |
| `formatVersion` | The JSON number `2`. |
| Pack and character `id` | 2–64 characters; lowercase letters, digits, `.`, `_`, or `-`; begins with a letter or digit. Character IDs are unique within a pack. |
| `name`, `license` | Non-empty text, maximum 120 characters. |
| `version` | Non-empty text, maximum 64 characters. |
| `type` | Exactly `"trainer"` or `"monster"`. This describes what the character is. |
| `level` | Optional integer from 1 through 999. Defaults to 5 during battle. |
| `maxHp` | Optional integer from 1 through 999. Defaults to 20 during battle. |
| `assignableTo` | Non-empty array containing `"contact"`, `"player"`, or both exactly once. |
| `frontImage`, `backImage` | Relative ZIP path to a `.png` or `.webp` file. |
| `callSound` | Relative ZIP path to an `.ogg` file. |
| `characters` | One to 200 entries. |

`type` and `assignableTo` are independent. A trainer or monster may be available to contacts,
the player, or both. Every character assignable to `player` must provide `backImage`; the player
side is rendered from behind. Contact assignments use `frontImage`.

Do not use `assignableTo` to infer `type`, and do not use `type` to restrict `assignableTo`.
MonsterDialer resolves the two types into separate battle slots:

| `type` | Contact-side artwork | Player-side artwork |
| --- | --- | --- |
| `trainer` | `frontImage`, used as the contact's trainer | `backImage`, used as the player's trainer |
| `monster` | `frontImage`, used as the contact's monster | `backImage`, used as the player's monster |

A pack may contain any mixture of trainers and monsters. A call can display one assigned trainer
and one assigned monster for the player, plus one assigned trainer and one assigned monster for
the contact. Therefore, never combine a trainer and its monster into one image or one manifest
entry. Give each independently selectable subject its own character entry, ID, type, and image
files.

`level` and `maxHp` affect monster battle data. Omit either field to use its default. They do not
change trainer rendering, even if included on a trainer entry.

## Archive contract

The ZIP must use this layout. `manifest.json` must be at the archive root—never inside a parent
directory. Paths use forward slashes and must not include absolute paths, `..`, duplicate files,
or empty path segments.

```text
<pack-file-name>.zip
├── manifest.json
├── art/
│   ├── mossling.png
│   └── mossling-back.png
└── audio/                         # only when a call sound is included
    └── mossling.ogg
```

Every path named by the manifest must exist exactly once in the ZIP. Keep the compressed archive
at or below 24 MB; each file at or below 8 MB; and uncompressed content at or below 48 MB.
Images must decode correctly, be no larger than 4096 × 4096 pixels, and contain no more than
16,777,216 pixels.

After all validation succeeds, delete the staging directory `test-packs/<pack-file-name>/` and
retain only `test-packs/<pack-file-name>.zip`. In the handoff, link or report only the ZIP file;
do not offer or reference the staging folder.

If `test-packs/<pack-file-name>.zip` already exists, replace it with the newly validated ZIP.
Use the same `<pack-file-name>` for the temporary staging directory and report only the final
ZIP.

## Required validation and handoff

Before reporting completion:

1. Verify the JSON parses and meets the manifest contract.
2. Verify every manifest media path exists in the ZIP.
3. Verify the ZIP has no duplicate paths and passes an integrity test.
4. Report the final absolute path to the ZIP and list the included character IDs.
