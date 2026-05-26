
<p align="center"><img src="logo.png" alt="Logo"></p>
<h1 align="center">GregTech CEu: Modern - StarT Fork</h1>
<p align="center">A community fork of GregTech CEu: Modern maintained by the StarT Development Team</p>
<h1 align="center">
    <a href="https://github.com/StarT-Dev-Team/GTM-StarT-Fork/blob/main/LICENSE"><img src="https://img.shields.io/github/license/StarT-Dev-Team/GTM-StarT-Fork?style=for-the-badge" alt="License"></a>
    <a href="https://github.com/StarT-Dev-Team/GTM-StarT-Fork/releases"><img src="https://img.shields.io/github/v/release/StarT-Dev-Team/GTM-StarT-Fork?style=for-the-badge" alt="Release"></a>
</h1>

## About This Fork

This is a community-maintained fork of [GregTech CEu: Modern](https://github.com/GregTechCEu/GregTech-Modern) by the StarT Development Team. 

**Original Mod:** [GregTech CEu: Modern](https://www.curseforge.com/minecraft/mc-mods/gregtechceu-modern)  
**Original Repository:** [GregTechCEu/GregTech-Modern](https://github.com/GregTechCEu/GregTech-Modern)

### Fork Status
This fork has diverged from upstream and does **not track versions 8.x.x and later**.

Development is now independent. Upstream fixes or improvements may be selectively
ported when they make sense for this fork, but version parity with upstream is
not a goal.

This fork should be considered a separate project with its own design goals.

### Fork Goals
#### Project Goals
- Maintain a version of GTm tailored for the needs of the Star Technology modpack.
- Improve configurability and pack-developer control.
- Keep mechanics consistent and intuitive for players.
- Prioritize maintainability over heavy mixins or fragile patches from addons.

#### Development Goals
- Revert or adjust upstream features that do not fit the intended gameplay design.
- Introduce new features that would otherwise require large external modifications.
- Improve internal APIs to make extending the mod easier.
- Add general quality-of-life improvements and bug fixes.

### Changes from Upstream (Non-Exhaustive List)

#### New Features
- Electric versions of blast furnaces and smokers
- Multiblocks can require specific energy hatch amperage (2A / 4A / 16A)
- Layered inputs (`LayeredWorkableElectricMultiblockMachine`)
- Advanced detector covers can optionally output strong redstone signals
- Advanced detector covers now have configurable cycle durations
- ULV components added
- ME hatches can connect to the same network via sides using a screwdriver
- Added assembler recipes for casings that were missing them
- Added machine mode cover
- ME stocking hatches now have configurable cycle durations
- Paginated tooltips
- Bottom tooltips (inserted after paginated tooltips)
- Automatic capabilities (recipe modifiers) tooltip for machines
- Automated generation of recipe types tooltip for machines, if there is more than one
- Added `eu_to_start` recipe condition and recipe modifier
- Multiblocks now display the reason why they are not forming

#### Gameplay & Balance Changes
- LCR coil benefits and parallel LCR support (configurable)
- Chance boosting now scales with recipe tier instead of speed overclocks (configurable)
- Steam boiler balance adjustments
- Large packer size changed from 6 blocks to 5 blocks long
- Enabled borosilicate glass plates and foils
- Energy converters now default to FE-to-EU mode
- Re-added the sodium-to-sodium-hydroxide recipe and increased its duration
- Increased duration of the formic acid recipe
- Multi Smelter now subticks
- Added batch mode to ABS
- Changed recipes for ME Pattern Buffer and ME Pattern Buffer Proxy to not require renaming

#### Configurability Improvements
- Multiblocks stalling on power loss is now configurable
- Super tanks acting as drums is now configurable
- ULV components can be enabled or disabled via config
- Minimum stocking cycle duration can now be configured

#### Restored / Reverted Upstream Changes
- Chance boosting restored to base recipes
- Reverted to older bauxite (rutile) processing line
- Restored long rod extruder mold
- Soft mallets can pause machines again
- Reverted maceration tower mob grinder behavior
- Reverted some upstream texture changes
- Reverted some Z-fighting fixes that worsened rendering issues
- Restored older distinct bus behavior

#### Technical / API Improvements
- TagPrefix blocks can now easily use falling block behavior
- Disabled auto-generation of recycling recipes after the KubeJS recipe event, which caused recycling recipes to be non-removable through KubeJS
- Materials now support `.colors(primary, secondary)` to set both colors at once
- Improved error message for using an incompatible category for a recipe type
- Assembly line recipes no longer fail if an input bus has a configured circuit set
- UI height now grows correctly for ME parts with more than 16 slots
- Made several ME part classes public for use in addons
- Added `OpticalComputationMachine` to enable proper use of CWU in other multiblocks and correct display in Jade
- UI slots now have lower priority than multiblock parts for multiblocks that support both a UI and I/O parts
- Research Station and `OpticalComputationMachine` machines account for CWU discount based on OC in XEI recipe viewer
- Refactored `RecipeRunner` to integrate older distinct bus behavior with colored inputs

#### Bug Fixes
- Fixed voltage display not handling MAX+X values correctly
- Fixed 2A energy hatches using incorrect 1A overlays
- Fixed inability to use RShift when viewing extended tooltips
- Fixed bronze armor being stronger than steel armor
- Fixed lamp blocks not being breakable with a pickaxe or wrench
- Fixed AOE on mining hammer consuming N+1 durability when mining N blocks
- Fixed idle and paused overlays for the assembler not matching the height of the active overlay
- Fixed torch recipe using creosote consuming the fluid container
- Fixed combustion generators displaying incorrect fuel usage information
- Fixed glass bottle recipe in extruder not registering
- Fixed some tooltip issues for tools
- Fixed battery buffers showing the input overlay on the sides
- Fixed "water bottle" fluid being registered in EMI
- Fixed hammer drops when the output is a TagPrefix that is not an ore
- Fixed `.tooltips()` not working correctly in KubeJS
- Fixed batching displaying `1x`
- Removed unused `Parallel_Hatch` recipe modifier from ABS
- Fixed wrench usage overlay not displaying correctly on multiblock parts
- Fixed cracker not working properly with tier 10+ coils
- Fixed `No energy` tooltip icon appearing in machines when it shouldn't
- Fixed Advanced Detector Cover data not persisting
- Fixed Pattern Buffers and Proxies acting as output buses/hatches when they don't have an output handler
- Fixed Configurable Maintenance Hatch being able to set recipe duration to 0t

#### UI & Texture Changes
- Voltage display format adjusted (EU/t and amperage order clarified)
- Fluid drills and miners added to XEI diagrams
- Auto-push buttons moved to the right side of the machine UI
- Mega Blast Chiller now uses the vacuum freezer overlay
- Improved filter slot in item buses
- Added abbreviations to multiblock names
- Changed some multiblock controller overlays to use proper transparent backgrounds
- Voltages in component names are now colored

## Versioning system

This fork follows the older versioning system of GTm (ex. 1.6.4) with some rules.
The versioning is loosely based on the update cycles of Star Technology. 
The second number represents the major version (`1.7.x` -> Theta update, `1.8.x` -> Iota update, etc.), while the third number represents the minor version (0-indexed, where `1.7.0` -> Theta 1, `1.7.1` -> Theta 2, etc.).
In case of this mod being updated for hotfix updates, this is represented by adding a lowercase letter of the Latin alphabet, incremented for every time it was updated in hotfix updates (ex. `1.7.0a`, `1.7.0b`).

## Credits

### Fork Development Team

- **trulyno** - Fork Maintainer
- **UltraPuPower1** - Developer
- **n1xx1** - Developer
- **Luzifer Senpai** - Developer
- **Kolja** - Developer

### Original GregTech CEu: Modern Team

This fork is based on the excellent work of the GregTech CEu: Modern development team:

- **KilaBash** - Original GTCEu Modern Developer
- **screret** - Original GTCEu Modern Developer
- **serenibyss** - Original GTCEu Modern Developer
- **Tech22** - Original GTCEu Modern Developer
- **YoungOnion** - Original GTCEu Modern Developer
- **Mikerooni** - Original GTCEu Modern Developer
- **Ghostipedia** - Original GTCEu Modern Developer

## For Developers

To add this fork as a dependency to your project, you have to use mavenlocal.

1. Fork and clone this repository locally
2. After the gradle setup is finalized, run the `publishing/publishToMavenLocal` task
3. In your project, make sure you have `mavenLocal()` added in your `build.gradle` `repositories` section
4. Refresh the project, and the dependency should work


### IDE Requirements (when using IntelliJ IDEA)

For contributing to this mod, the [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok) for IntelliJ IDEA is strictly required.  
Additionally, the [Minecraft Development plugin](https://plugins.jetbrains.com/plugin/8327-minecraft-development) is recommended.

## Credited Works

- Most textures are originally from [Gregtech: Refreshed](https://modrinth.com/resourcepack/gregtech-refreshed) by @ULSTICK. With some consistency edits and additions by @Ghostipedia.
- Some textures are originally from the **[ZedTech GTCEu Resourcepack](https://github.com/brachy84/zedtech-ceu)**, with some changes made by the community.
- New material item textures by @TTFTCUTS and @Rosethorns.
- Wooden Forms, World Accelerators, and the Extreme Combustion Engine are from the **[GregTech: New Horizons Modpack](https://www.curseforge.com/minecraft/modpacks/gt-new-horizons)**.
- Primitive Water Pump is from the **[IMPACT: GREGTECH EDITION Modpack](https://gt-impact.github.io/#/)**.
- Ender Fluid Link Cover, Auto-Maintenance Hatch, Optical Fiber, and Data Bank Textures are from **[TecTech](https://github.com/Technus/TecTech)**.
- Steam Grinder is from **[GregTech++](https://www.curseforge.com/minecraft/mc-mods/gregtech-gt-gtplusplus)**.
- Certificate of Not Being a Noob Anymore is from **[Crops++](https://www.curseforge.com/minecraft/mc-mods/berries)**.

See something we forgot to credit? Reach out to us by opening an issue and ask for appropriate credit, we will happily mark it here.

## License

This project is licensed under the LGPL-3.0 license - see the [LICENSE](LICENSE) file for details.

This fork maintains the same license as the original GregTech CEu: Modern project.
