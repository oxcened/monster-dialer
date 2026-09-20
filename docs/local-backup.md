# Local Backup

Local Backup creates a portable copy of MonsterDialer's character collection and local progress.
It is useful when moving to another device or keeping an offline copy before changing devices.
It does not require an account or a network connection.

## Create a backup

1. Open **Settings → Local backup**.
2. Select **Export backup**.
3. Choose a destination and keep the suggested `.monsterbackup` filename, or give the file another name ending in `.monsterbackup`.

The exported archive is a ZIP-compatible file with a small format manifest and the app's local
character data. Keep it somewhere you control. It may contain character artwork, character pack
metadata, selected character assignments, and Battle Journal entries.

## Restore a backup

1. Copy the `.monsterbackup` file onto the device or make it available through Android's file picker.
2. Open **Settings → Local backup** and select **Restore backup**.
3. Select the backup file and confirm the replacement warning.

Restoring replaces the current character collection and local progress with the backup's contents.
The screen recreates after a successful restore so the collection, progress counters, and journal
show their restored state.

## Included data

- Installed character packs and their local artwork and sounds
- Custom characters
- Player roster and contact-character assignments, including their local contact identifiers
- Radiant and other unlockable variant discoveries
- Player battle count and Battle Journal entries, including saved journal sprite thumbnails
- Character-pack enablement state

## Not included

- Android contact records, favourites, and call history
- Google account sessions and account email addresses
- Online Profile share links or remotely shared profiles
- Variant Backup records stored in Firebase

Contact-character assignments use identifiers derived from local contacts. Those identifiers are
included so assignments can be restored when the same contacts are present on the destination
device, but a backup does not copy contact records themselves. Treat the backup as private data.

## Safety and compatibility

Only restore backups you trust. Before replacing any local data, MonsterDialer validates the
archive format and rejects unsafe file paths, unsupported files, archives with more than 4,096
files, and archives larger than 128 MiB after extraction. The current local data remains in place
if validation or staging fails.

Local Backup is separate from [Variant Backup](variant-backup.md). Variant Backup is an optional,
account-based additive sync for unlockable variants; Local Backup is an offline, full replacement
restore for character collection and progress data.
