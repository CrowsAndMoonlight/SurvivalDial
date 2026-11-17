# Release candidate playtest

Automated checks cover the catalog, presets, numeric and cross-field validation, settings rollback, save isolation, and dialog Apply/Cancel through real API interfaces with mocked services. They do not render the UI or launch a campaign.

Before publishing:

1. Install the candidate into a clean 0.98.5a-RC7 profile and enable it. Confirm launcher loading without errors.
2. Choose Extended mode and Continue. Confirm the editor precedes occupation choices; Continue adds starting credits exactly once. Check new-game Escape behavior, back navigation, and abandoning setup for a new Normal game.
3. Expand the preset selector, select all four presets, adjust one slider, and confirm Custom. Check every category, long labels, tooltips, four-value arrays, and fractional Keep values at common screen resolutions and UI scales.
4. Set retirement to 50-60 cycles before generation. Confirm the retirement script uses those bounds. Check existing campaigns keep their original rolled retirement dates.
5. Open Ctrl+F8 on the campaign map, including while paused. Confirm it stays closed in combat, markets, menus, and other dialogs. Apply a fuel-capacity change and verify fleet capacity; test fuel consumption with a non-civilian cruiser and a tanker.
6. Check ship/weapon/fighter/commodity prices, resale, salaries, tariffs, hazard damage, and the next month's corruption calculation. Exit and reopen relevant screens after applying.
7. Change future-stock/drop controls and verify the next newly generated stock or encounter. Confirm existing stock, wrecks, and loot are preserved.
8. Cancel changes, save/reload two Extended campaigns with different settings, then load a Normal campaign in the same process. Verify no cross-save leakage. Test an existing Extended save without any Survival Dial data.
9. Test invalid bounds and restore Hard. Check new-game-only controls are disabled during play and live presets preserve them.
10. Test the user's actual mod list, especially Nexerelin's separate creation flow and mods affecting the same settings. Inspect `starsector.log` after each scenario.

11. Generate new Survival campaigns with the same seed at 50%, 75%, 100%, and 125% cloud coverage. Check the logged before/after counts, visible cloud coverage, protected system entrances/abyss, and cloud interaction while flying. Confirm 100% and Normal mode leave the vanilla map unchanged. Try 0% and a custom percentage.
12. Save/reload an adjusted campaign and check that cloud coverage is preserved without being applied again. Load a pre-0.1.2 save and verify the map stays unchanged. Restart without the mod and confirm the generated terrain still loads.

Do not label the release fully tested until these checks have been run in-game. Record game build, active mods, UI scale, results, and any settings whose observed effect differs from the audit.
