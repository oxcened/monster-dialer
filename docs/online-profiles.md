# Online Profiles

Online Profiles are opt-in public battle cards. MonsterDialer stores the relationship between a
contact number and an Online Profile only in the app-private `online-profile-links.json` file.
Phone numbers, phone-number hashes, contact names, and address books must never be written to
Firebase, analytics, crash reports, or logs.

## Firebase setup

Enable Anonymous Authentication, Cloud Firestore, and Cloud Storage in the Firebase project.
Deploy [`firebase/firestore.rules`](../firebase/firestore.rules) and
[`firebase/storage.rules`](../firebase/storage.rules). The checked-in rules validate every field
in the public schema, establish ownership, permit only single-document reads, and disable
listing. Before release, test them against the deployed Firestore emulator and enable Firebase
App Check enforcement for Authentication, Firestore, and Storage; rules cannot rate-limit
anonymous profile creation.

The public document path is `onlineProfiles/{publicProfileId}`. The profile ID is an opaque,
cryptographically random capability, not a Firebase UID. Sprite files use the fixed paths
`onlineProfiles/{publicProfileId}/sprites/trainer.png` and
`onlineProfiles/{publicProfileId}/sprites/monster.png`. Storage access is revoked when the
corresponding profile document is deleted, including when blob cleanup must be retried.

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
