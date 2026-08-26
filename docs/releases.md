# Releases

Monster Dialer is distributed as a signed APK attached to each GitHub Release.
Each release is built from the pushed Git tag, includes a SHA-256 checksum, and
has a GitHub artifact attestation. This makes the source revision, APK, and
published asset relationship explicit for sideloading users.

## Versioning

`appVersionName` in [`gradle.properties`](../gradle.properties) is the single
authoritative semantic version (`MAJOR.MINOR.PATCH`). The Android `versionCode`
is derived as `MAJOR * 1,000,000 + MINOR * 1,000 + PATCH`; it is therefore a
monotonically increasing integer for normal semantic-version releases. Minor
and patch values must each be 999 or less.

Before a release, change only `appVersionName`, commit it to `main`, and let CI
pass. The release tag must be exactly `v` followed by that value; for example,
`appVersionName=0.4.0` requires tag `v0.4.0`.

`opendialerRef` pins the compatible OpenDialer commit used in CI and releases.
Update it in a normal compatibility-tested change when intentionally adopting
upstream code. Do not change it as part of a tag-only release unless that
change has already passed CI.

## One-time GitHub configuration

Create these **Actions secrets** in the repository settings. Never commit a
keystore or any of these values.

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Alias of the signing key |
| `ANDROID_KEY_PASSWORD` | Password for that key |

On macOS or Linux, create the first value without line wrapping:

```bash
base64 < release-keystore.jks | tr -d '\n'
```

Copy the resulting single line into `ANDROID_KEYSTORE_BASE64`. Keep the
original keystore backed up securely: losing it prevents signing updates that
Android accepts as upgrades.

## Licensed font for CI

The app's licensed font is intentionally not tracked in Git. Place your
licensed copy at `app/src/main/res/font/ui_pixel_font.otf` for local builds.

CI and release builds restore the same file from a private asset location. Add
these Actions secrets:

| Secret | Value |
| --- | --- |
| `FONT_ASSET_URL` | Private download URL for the font asset |
| `FONT_ASSET_TOKEN` | Read-only token authorized to download that asset |
| `FONT_ASSET_SHA256` | SHA-256 digest of the original font file, without a filename |

For a private GitHub Release asset, use its API asset URL as
`FONT_ASSET_URL` and a fine-grained token with read-only **Contents** access to
the private asset repository as `FONT_ASSET_TOKEN`. Generate the checksum with:

```bash
shasum -a 256 app/src/main/res/font/ui_pixel_font.otf
```

The workflow verifies this digest after download and before Gradle runs. Do not
store the font itself in a GitHub Actions secret: it exceeds the secret size
limit and is not source material that can be publicly redistributed.

## Make a release

Use the helper script to prepare the version-change commit:

```bash
scripts/prepare-release.sh 0.4.0
```

After its CI run is green, publish the tag. The prompt defaults to **No**;
pass `--yes` only for deliberate non-interactive use.

```bash
scripts/prepare-release.sh 0.4.0 --publish
```

The script requires a clean, up-to-date `main` branch, validates the semantic
version, and rejects existing tags. Its preparation mode updates only
`appVersionName`, commits `chore(release): prepare v0.4.0`, and pushes `main`.

The `Android Release` workflow validates the tag against `appVersionName`,
checks out the pinned OpenDialer revision, decodes the keystore only on the
runner, builds the signed release APK, writes its SHA-256 checksum, attests the
assets, and creates a GitHub Release with generated notes.

Users can verify a downloaded APK with:

```bash
sha256sum -c MonsterDialer-v0.4.0.apk.sha256
```

## Troubleshooting

* **Tag/version validation failed:** update `appVersionName` on `main`, merge
  it, then tag the exact matching `vMAJOR.MINOR.PATCH` version.
* **Signing failed:** confirm all four secrets exist, the Base64 value was
  copied as one line, and the alias/password values open the original keystore.
* **OpenDialer checkout or build failed:** verify `opendialerRef` identifies an
  accessible compatible commit in `oxcened/opendialer`.
* **A release already exists:** GitHub rejects duplicate tag releases. Correct
  the issue in a new version and push a new tag; do not replace published APKs.

Local builds work without signing secrets. `./gradlew assembleRelease` creates
an unsigned release APK locally; CI uses the separate debug build, tests, and
lint checks.
