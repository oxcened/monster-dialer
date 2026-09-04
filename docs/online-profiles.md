# Online Profiles

Online Profiles are opt-in public battle cards. MonsterDialer stores the relationship between a
contact number and an Online Profile only in the app-private `online-profile-links.json` file.
Phone numbers, phone-number hashes, contact names, and address books must never be written to
Firebase, analytics, crash reports, or logs.

## Firebase setup

Enable Google as a Firebase Authentication provider, Cloud Firestore, and Cloud Storage in the
Firebase project. Add the Android app's SHA-1 signing-certificate fingerprint, then download the
updated `google-services.json` so it contains the generated `default_web_client_id` required by
Credential Manager. Do not enable Anonymous Authentication.
Deploy [`firebase/firestore.rules`](../firebase/firestore.rules) and
[`firebase/storage.rules`](../firebase/storage.rules). The checked-in rules validate every field
in the public schema, establish ownership, permit only single-document reads, and disable
listing. Before release, test them against the deployed Firestore emulator and enable Firebase
App Check enforcement for Authentication, Firestore, and Storage; rules cannot rate-limit
profile creation.

The public document path is `onlineProfiles/{publicProfileId}`. The profile ID is an opaque,
cryptographically random capability, not a Firebase UID. Sprite files use the fixed paths
`onlineProfiles/{publicProfileId}/sprites/trainer.png` and
`onlineProfiles/{publicProfileId}/sprites/monster.png`. Storage access is revoked when the
corresponding profile document is deleted, including when blob cleanup must be retried.

Each Google account also has one private document at `onlineProfileOwners/{firebaseUid}`. It
contains only the account's active opaque profile ID and a server timestamp. Rules allow only
that signed-in UID to directly read or modify its own index document; collection listing is
disabled. This makes Firestore the source of truth when the user signs in on another device,
without exposing a UID or storing an email in a public profile.

On an explicit Google sign-in or profile enable, the app reads this index to retain the existing
public ID and current revision, then publishes the signing device's current trainer and monster
to that ID. It does not import the remote character content into the device.

## Call behavior

The in-call UI only reads an already-cached profile. It starts an asynchronous refresh after a
call is displayed and retains the ordinary offline contact encounter if a profile is absent,
stale, malformed, or unavailable. Firebase is never used to initiate, answer, reject, route, or
otherwise control a GSM call.

## Linking

Opening `monsterdialer://profile/{publicProfileId}` creates a pending local link. Selecting a
contact stores every valid libphonenumber-normalized E.164 number for that contact against the
opaque profile ID. Invalid, anonymous, voicemail, and conference identities are not linked or
looked up.
