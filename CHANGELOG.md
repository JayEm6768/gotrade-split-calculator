# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-11-22

### Added
- Split calculator: enter a USD amount, add up to 20 tickers and assign percentages that total 100%.
- Preset percentage chips (5 to 100), a Rest chip that fills the remaining percentage, and a Custom option.
- Per-ticker results: slice, order amount to enter in GoTrade after fees, fees and leftover.
- Fee model with a percentage trading fee, a minimum trading fee and a per-buy regulatory fee, all editable in settings.
- Largest-remainder rounding so slices always add up to the amount exactly.
- Warning when fees exceed 1% of the amount, and handling for slices too small to cover fees.
- Saved combos that restore tickers and percentages in one tap without changing the amount.
- Home-screen widget showing your split, with a quick calculator popup when tapped.
- Light and dark themes.
- Terminal version of the calculator (`./gradlew :cli:run`).
- Unit tests for the fee math in the `core` module.

[1.0.0]: https://github.com/JayEm6768/gotrade-split-calculator/releases/tag/v1.0.0
