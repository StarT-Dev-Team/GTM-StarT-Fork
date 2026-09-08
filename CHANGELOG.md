# Changes from Upstream (Non-Exhaustive List)

## New Features
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

## Gameplay & Balance Changes
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

## Configurability Improvements
- Multiblocks stalling on power loss is now configurable
- Super tanks acting as drums is now configurable
- ULV components can be enabled or disabled via config
- Minimum stocking cycle duration can now be configured

## Restored / Reverted Upstream Changes
- Chance boosting restored to base recipes
- Reverted to older bauxite (rutile) processing line
- Restored long rod extruder mold
- Soft mallets can pause machines again
- Reverted maceration tower mob grinder behavior
- Reverted some upstream texture changes
- Reverted some Z-fighting fixes that worsened rendering issues
- Restored older distinct bus behavior

## Technical / API Improvements
- TagPrefix blocks can now easily use falling block behavior
- Materials now support `.colors(primary, secondary)` to set both colors at once
- Improved error message for using an incompatible category for a recipe type
- Assembly line recipes no longer fail if an input bus has a configured circuit set
- UI height now grows correctly for ME parts with more than 16 slots
- Made several ME part classes public for use in addons
- Added `OpticalComputationMachine` to enable proper use of CWU in other multiblocks and correct display in Jade
- UI slots now have lower priority than multiblock parts for multiblocks that support both a UI and I/O parts
- Research Station and `OpticalComputationMachine` machines account for CWU discount based on OC in XEI recipe viewer
- Refactored `RecipeRunner` to integrate older distinct bus behavior with colored inputs

## Bug Fixes
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

## UI & Texture Changes
- Voltage display format adjusted (EU/t and amperage order clarified)
- Fluid drills and miners added to XEI diagrams
- Auto-push buttons moved to the right side of the machine UI
- Mega Blast Chiller now uses the vacuum freezer overlay
- Improved filter slot in item buses
- Added abbreviations to multiblock names
- Changed some multiblock controller overlays to use proper transparent backgrounds
- Voltages in component names are now colored