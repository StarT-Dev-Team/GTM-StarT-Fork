
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

### Changes from Upstream (non-exhaustive list)
#### New Features
- Electric versions of Blast Furnaces and Smokers
- Multiblocks can require specific energy hatch amperage (2A / 4A / 16A)
- Multiblocks can support layered inputs (stepped recipes)
- Advanced redstone detectors can optionally output strong redstone signals
- ULV components added
- ME hatches can connect to the same network via sides by using a screwdriver

#### Gameplay & Balance Changes
- LCR coil benefits and parallel LCR support (configurable)
- Chance boosting now scales with recipe tier instead of speed overclocks (configurable)
- Steam boiler balance adjustments
- Large Packer size changed from 6 blocks long to 5 blocks long
- Enabled borosilicate glass plates and foils
- Energy converters now default to FE to EU mode

#### Configurability Improvements
- Multiblocks stalling on power loss is now configurable
- Super tanks acting as drums is now configurable
- ULV components can be enabled/disabled via config

#### Restored / Reverted Upstream Changes
- Chance boosting restored to base recipes
- Reverted to older bauxite (rutile) processing line
- Restored long rod extruder mold
- Soft mallets can pause machines again
- Reverted Maceration Tower mob grinder behavior
- Reverted some upstream texture changes
- Reverted some Z-fighting fixes that worsened rendering issues

#### Technical / API Improvements
- TagPrefix blocks can easily use falling block behavior
- Disabled auto generation of recycling recipes after KubeJS recipe event which caused recycling recipes to non-removable through KubeJS
- You can now use `.colors(primary, secondary)` to set both colors of a material
- Improved error message for using incompatible category for recipe type

#### Bug Fixes
- Fixed voltage display not handling MAX+X values correctly
- Fixed 2A energy hatches using incorrect 1A overlays
- Fixed the inability to use RShift when viewing extended tooltips
- Fixed bronze armor being stronger than steel armor
- Fixed lamp blocks not being able to be broken with a pickaxe and wrench
- Fixed AOE on mining hammer using N+1 durability when mining N blocks
- Fixed the idle and paused overlays for the assembler not being the same height as the active overlay
- Fixed the torch recipe using creosote consuming the fluid container
- Fixed combustion generators displaying incorrect information fuel usage
- Fixed glass bottle recipe in extruder not registering
- Fixed extra line for tools that don't have enchantments
- Fixed batter buffers showing the input overlay on the sides

#### UI and Texture Changes
- Voltage display format adjusted (EU/t and amperage order for clarity)
- Fluid drills and miners added to XEI diagrams
- Auto-push buttons were moved to the right side of the machine UI
- Mega Blast Chiller now uses the vacuum freezer overlay
- Improved filter slot in item buses

## Credits

### Fork Development Team

- **trulyno** - Fork Maintainer
- **KillLaAqua** - Developer
- **stellaurora** - Developer
- **n1xx1** - Dev helper
- **Luzifer Senpai** - Dev helper
- **Kolja** - Dev helper

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
