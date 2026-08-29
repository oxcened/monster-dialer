# MonsterDialer

MonsterDialer is an open-source Android phone app that turns calls into a retro monster-battle experience. Choose player characters, assign characters to contacts, and import custom character packs for personalised call screens.

MonsterDialer is built on [OpenDialer](https://github.com/oxcened/opendialer) and must be selected as the device's default phone app to make and receive calls.

[Join our community on Discord](https://discord.gg/hKXzFFMTFN)

## Features

- Phone, contacts, and call-history features provided by OpenDialer
- A portrait monster-battle screen during calls
- Separate player and contact character selections
- Importable character packs with custom artwork and optional call sounds
- Localised interface and community translations through [Crowdin](https://crowdin.com/project/monsterdialer)

## Install

Download the APK from the [latest GitHub release](https://github.com/oxcened/monster-dialer/releases/latest), install it on an Android device, then set MonsterDialer as the default phone app when prompted.

Releases include a SHA-256 checksum and GitHub build attestation. See the [release guide](docs/releases.md) for verification details.

## Build from source

### Prerequisites

- Android Studio with Android SDK 37 installed
- JDK 21
- A checkout of [OpenDialer](https://github.com/oxcened/opendialer) beside this repository
- A licensed copy of the UI Pixel Font at `app/src/main/res/font/ui_pixel_font.otf`

The Firebase configuration and font are not included in this repository. The font may not be redistributed; see the [release guide](docs/releases.md#licensed-font-for-ci) for its expected location. `app/google-services.json` is optional for local builds: when absent, Firebase Analytics and Crashlytics are inactive. CI and release builds provide it automatically.

### Checkout and run

Clone both repositories as siblings. MonsterDialer reads OpenDialer from `../opendialer` and pins the compatible revision in `gradle.properties`.

```bash
git clone https://github.com/oxcened/opendialer.git opendialer
git clone https://github.com/oxcened/monster-dialer.git monster-dialer
cd monster-dialer
git -C ../opendialer checkout "$(sed -n 's/^opendialerRef=//p' gradle.properties)"
./gradlew assembleDebug
```

Open `monster-dialer` in Android Studio or install `app/build/outputs/apk/debug/app-debug.apk` on a device. A physical device with phone capability is recommended for testing real calls.

To run the standard local checks:

```bash
./gradlew test lint assembleDebug
```

## Character packs

Character packs are ZIP archives containing a `manifest.json` file and their local assets. They can add player and contact trainers or monsters. Import them from **Settings → Character Packs**.

Read the complete [character-pack format guide](docs/character-packs.md) before creating or sharing a pack. Only import assets you created or are licensed to use.

## Contributing

Issues, pull requests, reviews, translations, and feature requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. You can propose and vote on ideas at [MonsterDialer Feedback](https://monsterdialer.fider.io/).

## Assets & Copyright

All visual assets bundled with MonsterDialer are original works created specifically for the project. These bundled assets are not sourced from third-party games or franchises. User-imported character packs are external content and are not bundled with or part of MonsterDialer.

## License

MonsterDialer is licensed under the [Apache License 2.0](LICENSE).
