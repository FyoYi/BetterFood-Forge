# Better Food Mod - AI Coding Guidelines

## Project Overview

This is a Forge mod for Minecraft 1.20.1 that enhances food mechanics with timestamp-based decay, immersive cooking, and realistic food preparation. Mod ID: `better_food`, package: `com.fyoyi.betterfood`.

## Architecture

- **Main Class**: `better_food.java` - Registers all mod components via DeferredRegister, handles client/server setup, and manages Forge/Mod event buses
- **Core Systems**:
  - Decay: Timestamp-based freshness using `FreshnessHelper.java` with percentage calculations (0-1.0)
  - Cooking: Physical interactions with dynamic cookedness (0-120%) managed by `CookednessHelper.java`
  - Effects: Freshness-based food effects in `FoodEffectHandler.java` with configurable bonuses
- **Data Storage**: NBT tags for item metadata (expiry times via "better_food_expiry", cookedness via "BetterFood_CookedProgress")
- **Configuration**: Runtime-configurable food properties in `FoodConfig.java`, mod settings in `Config.java`
- **Block Entities**: Shared `PotBlockEntity` for cooking pans/pots, individual entities for cutting boards

## Key Patterns

- **Registration**: Use `DeferredRegister` for items/blocks/entities in respective `ModXxx.java` files
- **Events**: `@SubscribeEvent` methods in dedicated event classes (Forge bus for gameplay, Mod bus for config)
- **Utilities**: Helper classes in `util/` for shared logic (time management with pause support, freshness calculations, cookedness handling)
- **Client/Server**: Separate client-side rendering in `client/renderer/`, item properties for dynamic textures
- **Recipes**: Category-based cooking system in `recipe/`, not fixed item IDs; dynamic recipes based on food tags and cookedness
- **Time Management**: Effective game time via `TimeManager.getEffectiveTime()` supporting pause functionality
- **Tooltip System**: Comprehensive item tooltips showing freshness, cookedness, food attributes, and dish evaluations

## Development Workflow

- **Build**: `./gradlew build` - Compiles and creates mod jar
- **Run Client**: `./gradlew runClient` - Launches Minecraft with mod (uses `run/` directory)
- **Run Server**: `./gradlew runServer` - Dedicated server testing (`--nogui` for headless)
- **Data Generation**: `./gradlew runData` - Generates recipes/tags (outputs to `src/generated/resources/`)
- **Debugging**: Check `run/logs/` and crash reports; use `runGameTestServer` for automated testing

## Conventions

- **Package Naming**: `com.fyoyi.betterfood.{component}` (e.g., `item`, `block`, `util`, `client.renderer`)
- **Class Naming**: PascalCase for classes, camelCase for methods/variables
- **Item Properties**: Custom items extend base classes with mod-specific behavior; use `IClientItemExtensions` for custom renderers
- **Time Units**: Use ticks (20/sec) for durations, convert from minutes in configs; `TICKS_PER_DAY = 24000L`
- **Freshness Logic**: Percentage-based (0-1.0) for decay states, not discrete stages; infinite lifetime = -1
- **Cookedness**: Float values 0-120%, stored in NBT, displayed as percentages
- **Food Tags**: Classification (e.g., "肉类"), features (e.g., "特点:生"), nature (e.g., "熟度:50.0%")
- **Event Handling**: Separate classes for different buses; use `@Mod.EventBusSubscriber` with bus specification

## Common Tasks

- **Add Item**: Register in `ModItems.java` with `DeferredRegister`, add model/texture in `resources/assets/better_food/`
- **Add Block**: Register in `ModBlocks.java`, create block entity in `ModBlockEntities.java` if interactive
- **Modify Decay**: Update `FoodConfig.java` for lifetimes and effects; use `FreshnessHelper.getFreshnessPercentage()`
- **Add Cooking Logic**: Use `CookednessHelper` for dynamic cookedness; implement in block entity interactions
- **Add Effects**: Hook into `FoodEffectHandler.java` for consumption events; configure bonuses in `FoodConfig`
- **Client Rendering**: Add renderers in `client/renderer/`, register in `ClientModEvents.registerRenderers()`
- **Add Tooltips**: Extend `ClientForgeEvents.onItemTooltip()` for custom item information display

## Examples

- **Freshness Check**: `float percent = FreshnessHelper.getFreshnessPercentage(level, stack);`
- **Cookedness Management**: `CookednessHelper.increaseCookedness(stack, 10.0f);`
- **Item Registration**: `public static final RegistryObject<Item> EXAMPLE = ITEMS.register("example", () -> new Item(new Item.Properties()));`
- **Event Handling**: `@SubscribeEvent public static void onEvent(EventType event) { /* logic */ }`
- **Time Usage**: `long effectiveTime = TimeManager.getEffectiveTime(level);`
- **NBT Storage**: `stack.getOrCreateTag().putLong("better_food_expiry", expiryTime);`
- **Tooltip Addition**: `event.getToolTip().add(Component.literal("Info").withStyle(ChatFormatting.GREEN));`</content>
  <parameter name="filePath">d:\MC Forge mod\forge-1.20.1-47.4.0-mdk\.github\copilot-instructions.md
