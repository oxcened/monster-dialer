# Variant Backup

Variant Backup is an opt-in, private backup for unlockable character variants. It protects
radiant discoveries today and is designed to cover other unlockable variant types added in the
future.

## How it works

Sign in with Google from **Share**, then select **Back up variants**. Signing in alone does not
turn on backup. Once enabled, the app synchronizes discovered variants whenever the account is
available and whenever a new variant is unlocked.

Each discovery is stored separately and identified by its character pack ID, character ID, and
variant ID. On another device signed into the same Google account, the app restores these records
and merges them with that device's local discoveries. The merge is additive: a valid local or
remote discovery is retained, so one device cannot overwrite another device's unlocked variants.

Backup records only identify unlocked variants. They do not include character artwork, character
pack files, contacts, phone numbers, call history, account email addresses, or Online Profile
share links. Variant Backup never publishes anything for other people to view.

## Availability and account changes

The app keeps the player's discoveries locally even when offline. It retries private
synchronization after the user signs in and when the app next has a usable connection.

Backing up requires the same Google account on each device. Signing out stops cloud
synchronization on that device but does not erase its local discoveries or the account's existing
private backup. Online Profile sharing is independent: enabling, regenerating, or removing a
shared profile does not change variant backup.

## Remote storage and access

Firestore stores records below `variantBackups/{firebaseUid}/unlocks/{unlockId}`. The Firebase
UID is only used as the private owner path; it is never included in a backup record. Firestore
rules permit only that authenticated account to read its records. Unlock records are append-only,
which preserves additive multi-device synchronization.
