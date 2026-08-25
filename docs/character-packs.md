# Building a MonsterDialer character pack

A character pack is a ZIP file containing one UTF-8 `manifest.json` and the local artwork and
sounds referenced by that manifest. It contains data only: no code, remote URLs, or contact data.

This guide describes pack format exactly as the app imports it.

## Fast path

1. Create a folder containing `manifest.json` and an `art` folder.
2. Add a front image for every character assignable to a contact. Add a genuinely different
   rear image for every character assignable to the player.
3. Copy the manifest below and replace every example value.
4. ZIP the **contents** of the folder, not the folder itself.
5. Verify the archive, then import it from **Settings → Character Packs → Import**.

The finished ZIP must open directly to `manifest.json`, not to another enclosing folder.

```text
my-pack.zip
├── manifest.json
├── art/
│   ├── forest-guide-front.png
│   ├── forest-guide-back.png
│   ├── mossling-front.png
│   └── mossling-back.png
└── audio/
    └── mossling.ogg
```

## Complete manifest example

JSON does not allow comments or trailing commas. Property names and enum values are
case-sensitive. Optional properties should be omitted when unused, not set to an empty value.
Unknown properties cause the entire pack to be rejected.

```json
{
  "formatVersion": 1,
  "id": "com.example.forest-friends",
  "name": "Forest Friends",
  "version": "1.0.0",
  "license": "Original artwork, used with permission",
  "creator": "Example Studio",
  "characters": [
    {
      "id": "forest-guide",
      "name": "Forest Guide",
      "type": "trainer",
      "assignableTo": ["contact", "player"],
      "frontImage": "art/forest-guide-front.png",
      "backImage": "art/forest-guide-back.png"
    },
    {
      "id": "mossling",
      "name": "Mossling",
      "type": "monster",
      "assignableTo": ["contact", "player"],
      "frontImage": "art/mossling-front.png",
      "backImage": "art/mossling-back.png",
      "level": 12,
      "maxHp": 45,
      "isRadiant": true,
      "callSound": "audio/mossling.ogg"
    }
  ]
}
```

## Manifest fields

### Pack fields

| Field | Required | Exact rule |
| --- | --- | --- |
| `formatVersion` | Yes | JSON integer `1`. No other version is accepted. |
| `id` | Yes | 2–64 characters matching `[a-z0-9][a-z0-9._-]{1,63}`. Use a stable, globally distinctive ID such as reverse-domain notation. |
| `name` | Yes | Non-blank string, at most 120 characters. |
| `version` | Yes | Non-blank string, at most 64 characters. Semantic versioning such as `1.0.0` is recommended but not required. |
| `license` | Yes | Non-blank string, at most 120 characters. State the licence or permission covering the pack's assets. |
| `creator` | No | JSON string naming the creator. Omit it if no creator should be shown. |
| `characters` | Yes | JSON array containing 1–200 character objects. |

Importing another ZIP with the same pack `id` replaces that pack. Its enabled/disabled state is
preserved. Changing the ID installs a separate pack. The app does not compare or order the
`version` value; it is descriptive metadata.

### Character fields

| Field | Required | Exact rule |
| --- | --- | --- |
| `id` | Yes | 2–64 characters matching `[a-z0-9][a-z0-9._-]{1,63}` and unique inside the pack. |
| `name` | Yes | Non-blank string, at most 120 characters. |
| `type` | Yes | Exactly `"trainer"` or `"monster"`. |
| `assignableTo` | Yes | Non-empty array containing `"contact"`, `"player"`, or both. Each value may appear only once. |
| `frontImage` | Sometimes | Exact, relative ZIP path to a `.png` or `.webp` image. Required whenever `assignableTo` contains `"contact"`; otherwise optional. |
| `backImage` | Sometimes | Relative path to a `.png` or `.webp` image. Required whenever `assignableTo` contains `"player"`; otherwise optional. |
| `level` | No | JSON integer from 1 through 999. A monster defaults to level 5 when omitted. |
| `maxHp` | No | JSON integer from 1 through 999. A monster defaults to 20 maximum HP when omitted. |
| `isRadiant` | No | Boolean, defaulting to `false`. Only valid for monsters. When `true`, the monster displays a radiant sparkle animation on entering battle. |
| `callSound` | No | Relative ZIP path to an `.ogg` file. The importer accepts and stores it, but the current call UI does not yet play it. |

`level` and `maxHp` must be integers, not quoted strings or decimals. Zero, negative numbers, and
four-digit values are invalid. They affect monster battle data; trainer rendering ignores them.
Current HP starts at the resolved `maxHp` value.

## Trainer, monster, player, and contact are independent

`type` says **what the character is**. `assignableTo` says **where the user may select it**. Do
not infer one from the other. Trainers and monsters may each be assignable to contacts, the
player, or both.

| Character configuration | Artwork the app uses |
| --- | --- |
| Contact trainer | Trainer `frontImage` in the contact-side trainer slot. |
| Player trainer | Trainer `backImage` in the player-side trainer slot. |
| Contact monster | Monster `frontImage` in the contact-side monster slot. |
| Player monster | Monster `backImage` in the player-side monster slot. |

Trainer and monster selections are separate. A call may show a trainer and a monster for the
player and another trainer and monster for the contact. Never combine a trainer and monster into
one image or manifest entry if users should be able to select them independently.

For a contact-only character, this is valid and needs no rear image:

```json
{
  "id": "mossling",
  "name": "Mossling",
  "type": "monster",
  "assignableTo": ["contact"],
  "frontImage": "art/mossling-front.png"
}
```

For a player-only character, this is valid and needs no front image:

```json
{
  "id": "mossling-player",
  "name": "Mossling",
  "type": "monster",
  "assignableTo": ["player"],
  "backImage": "art/mossling-back.png"
}
```

## Artwork and sound requirements

- Front artwork must show the subject from the contact/opponent side.
- Rear artwork must be a true view from behind. Do not reuse, mirror, or slightly rotate the
  front image.
- Transparent backgrounds are strongly recommended. Do not include names, labels, scenery,
  borders, logos, or watermarks unless they are intentionally part of the character artwork.
- Each image must decode as PNG or WebP, be no wider or taller than 4096 pixels, and contain no
  more than 16,777,216 pixels total (`width × height`). There is no required minimum size.
- Each media file may be at most 8 MiB.
- Sounds must be Ogg files with the `.ogg` extension.
- File extensions are checked, and every referenced file must exist at the exact same
  case-sensitive path in the ZIP.

Use only names, artwork, audio, and other material that you created or have permission to
redistribute. A licence string in the manifest does not create those rights. Do not distribute
copied game sprites, franchise characters, logos, or sounds without permission from their
rights holders.

## ZIP rules and limits

- `manifest.json` must be at the ZIP root with that exact lowercase name.
- All paths must be relative and use `/`, never `\`.
- Paths must not begin with `/` or `\` and must not contain empty segments, `.` segments, or
  `..` segments.
- Duplicate ZIP paths are invalid.
- Every file referenced by the manifest must be present.
- Every non-directory entry must be either `manifest.json` or a file referenced by the manifest.
  Extra files, including executables, scripts, and unsupported formats, cause import to fail.
- The ZIP may contain at most 512 entries, including directory entries.
- The compressed ZIP may be at most 24 MiB.
- Its total uncompressed file content may be at most 48 MiB.
- `manifest.json` may be at most 256 KiB.

## Create the ZIP

On macOS or Linux, open a terminal **inside the staging folder** and run:

```sh
zip -r ../my-pack.zip manifest.json art audio
```

If the pack has no `audio` folder, omit `audio` from the command:

```sh
zip -r ../my-pack.zip manifest.json art
```

On Windows PowerShell, open PowerShell **inside the staging folder** and run:

```powershell
Compress-Archive -Path manifest.json,art,audio -DestinationPath ..\my-pack.zip
```

Omit `audio` when that folder does not exist. If the destination ZIP already exists, delete or
rename it first so stale entries cannot survive from an earlier build.

Do not ZIP the parent directory. This is wrong:

```text
my-pack.zip
└── my-pack/
    └── manifest.json
```

## Validate before sharing

Run all of these checks:

1. Parse the manifest:

   ```sh
   python3 -m json.tool manifest.json
   ```

2. Test ZIP integrity:

   ```sh
   unzip -t ../my-pack.zip
   ```

3. List the archive and confirm `manifest.json` is at its root:

   ```sh
   unzip -Z1 ../my-pack.zip
   ```

4. Confirm every path in `frontImage`, `backImage`, and `callSound` appears exactly in that list.
5. Import the ZIP in the app. This final step performs the authoritative schema, size, path, and
   on-device image-decoding checks.

## Import and test

1. Put the ZIP on the device. For an Android emulator, either drag it onto the emulator window
   or run `adb push my-pack.zip /sdcard/Download/`.
2. Open **Settings → Character Packs → Import** and select the ZIP.
3. Confirm the pack is enabled.
4. Open **Settings → Player Character** and verify every player-assignable trainer and monster,
   especially that rear artwork is shown for player choices.
5. Open **Settings → Contact Characters**, choose a contact, and verify every
   contact-assignable trainer and monster.
6. Place or receive a test call and confirm the correct trainer, monster, level, and maximum HP
   render on each side.

If import fails, first check the exact manifest property names, JSON value types, ZIP root,
case-sensitive media paths, image decoding, and the numeric and archive limits above.
