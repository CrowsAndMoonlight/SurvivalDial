# Changelog

## 0.1.1 - simpler menu

- Show presets first, with optional common controls and a separate full-settings menu.
- Add plain names, before/after review, and Normal comparisons for verified values.
- Hide unchanged or inactive defaults, preserving access to older custom values.
- Keep neutral fuel-use components unchanged in presets.
- Edit the retirement window together and validate it before accepting changes. New-campaign presets use 45-50 cycles on Easy, 35-40 on Medium, 25-30 on Hard, and 20-25 on Insane.
- Put a generated preset comparison immediately after the README intro; move build instructions into developer documentation.

## 0.1.0 - release candidate

- Catalog all 143 Extended settings from 0.98.5a-RC7.
- Add native configuration during vanilla character creation and via Ctrl+F8 during an Extended campaign.
- Add Easy, Medium, Hard, and Insane preset selection, custom sliders, explanatory tooltips, and boolean switches.
- Preserve per-save settings with validation, Apply/Cancel, cache invalidation, and restoration between campaigns.
- Include a reproducible local build, behavioral tests, and an installable ZIP packager without game libraries.
