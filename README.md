# Split Calculator

An Android app that splits an amount across several tickers and tells you what to enter in GoTrade once fees are taken out.

[![CI](https://github.com/JayEm6768/gotrade-split-calculator/actions/workflows/ci.yml/badge.svg)](https://github.com/JayEm6768/gotrade-split-calculator/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/github/license/JayEm6768/gotrade-split-calculator)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/JayEm6768/gotrade-split-calculator)](https://github.com/JayEm6768/gotrade-split-calculator/releases/latest)

## Features

- Enter an amount in USD and add tickers (any number, up to 20).
- Pick each percentage with preset chips (5 to 100), Rest, or Custom. The total must be exactly 100%.
- For every ticker you see the slice, the order amount to enter in GoTrade after fees, the fees, and the leftover.
- Saved combos: one tap restores the tickers and percentages. The amount is left alone.
- Home-screen widget that shows your split. Tapping it opens a quick calculator popup.
- Fee settings you can edit, for when your rates differ from the defaults.
- Light and dark theme.

Requires Android 8.0 or newer.

## How the fees are calculated

The defaults model the buy side on GoTrade Global. All of them can be changed in the app.

- Trading fee: `max(rate x order, $0.10)`, with a default rate of 0.3%.
- Regulatory fee (FINRA CAT): $0.01 per buy.

Fees are charged on top of the order, so `order + fees <= slice`. The app finds the largest order that still fits in the slice.

Rounding:

- Slices use largest-remainder rounding, so they add up to the amount exactly.
- Order amounts are rounded down to the cent and fees are rounded up.

Example: $20 split as VOO 45 / SMH 25 / SCHD 15 / IBIT 15.

| Ticker | Percent | Slice  | Order | Fees  |
| ------ | ------- | ------ | ----- | ----- |
| VOO    | 45%     | $9.00  | $8.89 | $0.11 |
| SMH    | 25%     | $5.00  | $4.89 | $0.11 |
| SCHD   | 15%     | $3.00  | $2.89 | $0.11 |
| IBIT   | 15%     | $3.00  | $2.89 | $0.11 |

Total fees are $0.44 (2.2% of the amount). On small orders the $0.10 minimum dominates, so fewer tickers or a bigger amount brings the percentage down. The app warns you when fees go over 1%.

Notes:

- The trading rate varies by country (0.15% to 0.3%). Check your GoTrade app and set it in the settings.
- Current fee schedule: https://www.heygotrade.com/en/fee/
- Sell-side fees (SEC, TAF) are not included, since the app is meant for buying.

## Install

1. Download the APK from the [Releases](https://github.com/JayEm6768/gotrade-split-calculator/releases) page.
2. Allow installing from your browser or files app when Android asks.
3. Open the file and install. Play Protect may warn you because the app is not from the Play Store.

Check the download against the SHA-256 listed in the release notes.

Windows:

```
certutil -hashfile split-calculator-v1.0.0.apk SHA256
```

Linux and macOS:

```
sha256sum split-calculator-v1.0.0.apk
```

To update, install the new APK from this repo's releases. It is signed with the same key, so it installs over the existing app. An APK from anywhere else will not.

## Privacy

- The app requests no permissions and has no internet access.
- Combos and settings stay on your device in app-private storage.
- Backups and device transfer are disabled.
- Release builds are minified and signed.
- It only uses AndroidX, Google and JetBrains libraries.

See [SECURITY.md](SECURITY.md) for how to report a problem.

## Building from source

You need JDK 17 and the Android SDK 35.

```
./gradlew :core:test :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The debug APK ends up in `app/build/outputs/apk/debug/`.

Release signing is optional. Create your own keystore, then add a git-ignored `keystore.properties` in the project root with a single line pointing at a properties file kept elsewhere:

```
path=/absolute/path/to/signing.properties
```

That file holds the signing details:

```
storeFile=/absolute/path/to/my-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

If `keystore.properties` or the file it points to is missing, `assembleRelease` produces an unsigned APK.

The same calculator also runs in a terminal:

```
./gradlew :cli:run
```

## Project structure

| Module | What it is |
| ------ | ---------- |
| `core` | Pure Kotlin fee math with unit tests. No Android dependencies. |
| `app`  | The Android app: Jetpack Compose and Material 3 UI, Glance widget, DataStore for saved data. |
| `cli`  | Terminal version of the calculator built on `core`. |

## Contributing

Issues and pull requests are welcome. If a number looks wrong, include the amount, tickers, percentages and fee settings you used.

## License

[MIT](LICENSE)

## Disclaimer

This is an unofficial project. It is not affiliated with or endorsed by GoTrade. Fee figures are estimates and may change. Nothing here is financial advice.
