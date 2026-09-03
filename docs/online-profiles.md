# Online Profiles

Online Profiles are opt-in public battle cards. MonsterDialer stores the relationship between a
contact number and an Online Profile only in the app-private `online-profile-links.json` file.
Phone numbers, phone-number hashes, contact names, and address books must never be written to
Firebase, analytics, crash reports, or logs.

## Firebase setup

Enable Anonymous Authentication, Cloud Firestore, and Cloud Storage in the Firebase project.
Deploy [`firebase/firestore.rules`](../firebase/firestore.rules) and
[`firebase/storage.rules`](../firebase/storage.rules). Production rules should additionally
validate every nested profile field before launch; the checked-in rules establish ownership,
public single-document reads, and disabled listing.

The public document path is `onlineProfiles/{publicProfileId}`. The profile ID is an opaque,
cryptographically random capability, not a Firebase UID. Sprite files are PNGs beneath
`onlineProfiles/{publicProfileId}/sprites/{sha256}.png`.

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
