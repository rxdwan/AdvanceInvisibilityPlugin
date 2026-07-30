# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
## [1.2.0] - 2026-07-30
### Added
- **Configurable Sound Suppression**: Added a `suppress-sounds` block in `config.yml` allowing server admins to toggle specific sound suppressions (hurt, armor-equip, eating, drinking, burp, block-place, water-bucket).
- **World Pausing**: Invisibility effect is now paused when traveling to other dimensions (Nether/End) and automatically resumes upon returning to the Overworld.
- Renamed `attack-reveal` config section to `reveal-window`.


## [1.1.0] - 2026-07-26
### Added
- **Attack Reveal Window**: Added a fair-play feature where attacking another player while invisible temporarily reveals you for a configurable amount of time (`attack-reveal` in `config.yml`). During this window, you are partially visible and vulnerable.
- `attack-reveal.enabled` and `attack-reveal.duration` settings in `config.yml`.

## [1.0.1] - 2026-07-25
### Added
- Color customization for timer texts in Boss Bar and Action Bar (`boss-bar-color`, `boss-bar-style`, `boss-bar-text`, `action-bar-text`).
- `show-expired-title` toggle in `config.yml` to disable the expiration title on screen for Boss Bar and Action Bar users (while keeping it active for `NONE` display-type users).

### Fixed
- **Critical Stealth Bug**: Fixed an issue where mobs that aggroed a player *before* they gained the invisibility effect would not drop their aggro.
- Fixed an issue where the title warning threshold messages were being displayed when `display-type` was set to `BOSS_BAR` or `ACTION_BAR`. They now correctly only show when set to `NONE`.

## [1.0.0] - 2026-07-24
### Added
- Initial release of AdvancedInvisibility.
- Basic commands (`/advanceinv`), Vault economy integration, and stealth module.
- ProtocolLib-powered packet cancellation for entity rendering, fire particles, and sounds.
