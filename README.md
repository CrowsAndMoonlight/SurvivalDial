# Survival Dial

Make Starsector's Survival / Extended mode as forgiving or demanding as you like. Pick a preset, or optionally customize it with explained sliders. Normal mode is unchanged. No other mods required. Designed to be save-game compatible; removal testing is still pending.

<!-- presets:start -->

| Setting | Easy | Medium | **Hard (default)** | Insane |
| --- | --- | --- | --- | --- |
| Ship prices (frigate / destroyer / cruiser / capital) | 2 / 2 / 2.25 / 2.75x | 3.4 / 3.4 / 4 / 5.2x | 5 / 5 / 6 / 8x | 7.5 / 7.5 / 9 / 12x |
| Weapon prices | 3.25x | 6.4x | 10x | 15x |
| Fighter LPC prices | 2x | 3.4x | 5x | 7.5x |
| Crew hiring cost | 3.25x | 6.4x | 10x | 15x |
| Crew wages | 1.5x | 2.2x | 3x | 4.5x |
| Fuel price | 1.5x | 2.2x | 3x | 4.5x |
| Ship fuel capacity | 87.5% | 70% | 50% | 33.33% |
| Tanker fuel capacity | 83.75% | 61% | 35% | 23.33% |
| Fuel use (frigate / destroyer / cruiser / capital) | 1 / 1 / 1.125 / 1.25x | 1 / 1 / 1.3 / 1.6x | 1 / 1 / 1.5 / 2x | 1 / 1 / 2.25 / 3x |
| Commission income | 77.5% | 46% | 10% | 6.67% |
| Post-battle ship recovery factor | 80% | 52% | 20% | 13.33% |
| Story points per level | 2 | 2 | 2 | 2 |
| Retirement window | 45-50 cycles | 35-40 cycles | 25-30 cycles | 20-25 cycles |

<!-- presets:end -->
**Want more control?** Open **Customize → All Settings** to also adjust:

- **Markets:** stock availability, resale values, storage costs, hullmod and Domain-grade Parts prices, tariffs, and black-market bribes.
- **Personnel and support:** marine hiring and wages, officer combat pay, officer/admin availability, the Academy stipend, and task-force/resupply costs.
- **Salvage:** loot amounts, AI-core and special-item drops, wreck quality, and story-point ship recovery costs.
- **Colonies:** construction costs and time, growth, corruption, founding resources, stockpiles, and custom production limits.
- **Travel and encounters:** survey requirements, hazard and Transverse Jump damage, cargo scans, mission requirements, and selected encounter difficulty.
- **Career:** rejuvenation bills, spacer spending and debt, story points after the level cap, and bonus XP for selected story-point actions.

Some controls are available only during new-game setup; others affect future stock or rewards. Each control explains when it takes effect.

Multipliers and percentages are relative to Normal mode; recovery is a modifier, not a guaranteed recovery chance. These are selected new-campaign preset values. Story points stay at 2 per level unless customized. Retirement timing applies to new campaigns only. Hard restores vanilla Survival settings.

## Install

1. Download **SurvivalDial-0.1.1.zip** from this repository's **Releases**.
2. Extract it into `Starsector/mods` so you have `mods/SurvivalDial/mod_info.json`.
3. Enable **Survival Dial** in the launcher and start the game.

Configure it when starting Survival mode, or press **Ctrl+F8** on the campaign map. The ZIP includes the compiled mod; no commands or compiling needed.

For **Starsector 0.98.5a-RC7**. [Full settings inventory](docs/setting-inventory.md) / [Developer notes](docs/development.md)