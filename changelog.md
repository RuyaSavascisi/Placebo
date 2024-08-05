## 9.3.5
* Expanded `DeferredHelper` to cover various common cases without resorting to usage of the supplier overloads.
* Documented all methods of `DeferredHelper`.
* Made immediately-resolvable types (i.e. data component types) bypass uses of `DeferredHolder` when using `DeferredHelper`.
* Gave `ITabFiller` access to the full `BuildCreativeModeTabContentsEvent` for use with data registries.

## 9.3.4
* Fixed supporter wings not rendering.

## 9.3.3
* Made `DynamicRegistry#beginReload` and `DynamicRegistry#onReload` called on SSP hosts with the original registry data.
  * This fixes a bug where json brewing mixes were not being populated to JEI.

## 9.3.2
* Fixed Dynamic Registries not registering stream codecs.
* Fixed a crash that would occur if a JsonMix was instantiated.
* Added a registration helper for data components.

## 9.3.1
* Fixed a dedicated server crash.

## 9.3.0
* Ported to 1.21
* Added `StreamCodec` support to `DynamicRegistry`.
* Renamed `ItemAdapter` to `OptionalStackCodec`.
* Removed the `/placbeo string_to_obj` command.
* Removed `PotionIngredient`, `PacketDistro`, `LootSystem`, `PoolBuilder`, and `BrewingRecipeRegistry`.

## 9.2.2
* Fixed `ButtonClickMessage`'s ID being null.

## 9.2.1
* Fixed the name of `QuickMoveMenu#moveItemStackTo` being incorrect.

## 9.2.0
* Fixed `StackLootEntry.TYPE` using static registration.
* Added datagen helper classes `RuntimeDatagenHelpers` and `DynamicRegistryProvider`.
* Removed `MenuUtil.merge` and `MenuUtil.split`.
  * After https://github.com/neoforged/NeoForge/pull/599 a single dataslot can sync a full int.
* Made `TickingBlockEntityType` use a `TickSide` enum instead of two booleans.
* Updated and/or added documentation for various objects.

## 9.1.3
* Updated the generated unlocalized name for the new `singlePotion` / `multiPotion` helpers.

## 9.1.2
* Fixed `brewing_mixes` not loading.

## 9.1.1
* Added the `brewing_mixes` json loader, which requires less boilerplate to add brewing recipes.

## 9.1.0
* Added more `DeferredHelper` methods for creating potions.
* Removed the deprecated `collections` package.
* Added the `wanderer_trades` json loader, and config options to remove all non-json trades.
* Added the `brewing_recipes` json loader. Does not permit adjusting vanilla recipes or other mods' recipes.

## 9.0.6
* Fixed synced dynamic registries not working in singleplayer.
* Fixed placebo's primary itemstack codec not functioning.

## 9.0.5
* Added the `ResourceReloadEvent`, which fires on both client and server resource reload.
* Made the `NeoForge.EVENT_BUS` initialized at mod construction.

## 9.0.4
* Fixed an incompatibility with newer neo versions.

## 9.0.3
* Eagerly prevent registration of duplicate payload providers.

## 9.0.2
* Fixed PayloadProvider(s) not being registered.
* Made PayloadProvider versions mandatory, starting at `1` for all payloads.

## 9.0.1
* Fixed mixins not loading.

## 9.0.0
* Initial port to 1.20.4.

## 8.6.1
* Added `speed` param to `GradientColor`.

## 8.6.0
* Added back `EnchantmentUtils.getLevelForExperience` which was missed in 8.5.9.

## 8.5.9
* Updated EnchantmentUtils to be more useful.
  * New version is based on Bookshelf, old version was based on OpenBlocks.

## 8.5.8
* Fixed RandomAttributeModifier not being re-applicable.

## 8.5.7
* Fixed `ChatComponentMixin` not being in the client mixin package.
* Fixed the Supporter Trail / Wing keybinds causing a crash if pressed before entering the world.

## 8.5.6
* Fixed an issue with RandomAttributeModifier encountering UUID collisions.

## 8.5.5
* Fixed supporter keybinds not working.

## 8.5.4
* Made `ChancedEffectInstance`, `RandomAttributeModifier`, and `StepFunction` records, and added constant codecs which remove random values.
* Deprecated some old GSON code that should no longer be used.

## 8.5.3
* Fixed `ItemAdapter`'s item stack codec using `CompoundTag.CODEC` instead of `NBTAdapter.EITHER_CODEC`.
  * This meant that nothing was able to use legacy nbt-strings, which isn't entirely a bad thing, but not something that should be enforced yet.
  
## 8.5.2
* Fixed startup error from GearSetRegistry.

## 8.5.1
* Fixed how `CodecMap`'s default codec system worked.

## 8.5.0
* Removed `PSerializable` and usages. Everything is codecs now!
* Renamed `SerializerMap` to `CodecMap` and switched the internals to work with codecs.
* Added `NullableFieldCodec` to work around issues with `Codec#optionalFieldOf` - Stolen from Commoble.
* Added `GearSet` and `ChancedEffectInstance` from Apotheosis.
  * The `GearSetRegistry` was also pulled here. No default gear sets were added.

## 8.4.1
* Added the key as context to `DynamicRegistry#validateItem`.

## 8.4.0
* Removed `TypeKeyed` and usages. The keys of dynamic items may now be retrieved from the relevant `DynamicRegistry` or `DynamicHolder`.

## 8.3.7
* Added `DynamicRegistry#holderCodec` to easily produce a codec for `DynamicHolder`s.

## 8.3.6
* Added `/placebo hand` for emitting the currently held item in JSON Object form, which is used by dependent mods.
* Added `/placebo string_to_obj` for converting legacy NBT-Strings to JSON Object form.
* Made it so that `\n` is unescaped when logging chat lines.

## 8.3.5
* Removed forge dependency line from the mods.toml and marked as Forge and NeoForge for CF.
  * The dependency will be added back and the Forge marker will be removed once CF supports Neo correctly.

## 8.3.4
* Made DynamicRegistry use a linked hash map when syncing to preserve registration order.

## 8.3.3
* Made CachedObject lazy-init the ConcurrentHashMap stored on ItemStack.

## 8.3.2
* Renamed the MenuUtil.type overloads because generic inference fails.

## 8.3.1
* Added a T -> DynamicHolder function.
* Added support for empty holders.

## 8.3.0
* Major refactor to PlaceboJsonReloadListener and related code.
  * Now called DynamicRegistry, and DynamicRegistryObject is now DynamicHolder.
* Made data listeners trigger on the clientside when using PlaceboContainerMenu.

## 8.2.2
* Added the method `MessageProvider#getNetworkDirection`.

## 8.2.1
* Reduced `MessageHelper.handlePacket` from `Supplier<Runnable>` to `Runnable`.
* Added a quick util method for making `SoundEvent`s to `DeferredHelper`.

## 8.2.0
* Refactors and documentation updates to most systems.

## 8.1.5
* Added additional utils to ContainerUtil for creating MenuType(s).

## 8.1.4
* Added DeferredHelper and moved registry helper classes to the registry package.

## 8.1.3
* Added ITabFiller and TabFillingRegistry

## 8.1.2
* Added the atlas file which loads textures from /blocks and /items.

## 8.1.1
* Fixed coremods using the wrong package name.

## 8.1.0
* Updated to Neo.
* Updated buildscripts to automate coremods.json and mixins.json file.
* Added publishing tasks.

## 8.0.0
* Updated to 1.20.1

## 7.3.1
* Added SlotUpdateListener so I can stop copying `Runnable listener` into all my menu classes.

## 7.3.0
* Added an Ingredient codec.
* Made CachedObject use ConcurrentHashMap to avoid issues resolving offthread (induced by FastSuite recipe matching in parallel).
* Reduced conditions-not-met log message from PlaceboJsonReloadList

## 7.2.1
* Added a method to invalidate all cached objects of a given type.

## 7.2.0
* Updated TypeKeyed to no longer require setting the serializer. Objects should be aware of what their serializer is, as two serializers should not produce the same object.
  * Breaking change for Apotheosis, Gateways, and HNN, so all of those will need to be updated as well.
  * Also improved some validation in the deserialization process.
* Added more codec support across the board.

## 7.1.8
* Made the TagParser accept codec-form CompoundTag (NBT) Objects.
* Fixed a crash that could happen if negative luck was encountered.
* Added new Supporter Wings (Armored, Blaze, Bone, Cloud, Fairy, Mechanical, and Monarch).
* Added CachedObject, which allows for caching of nbt-encoded itemstack objects.

## 7.1.7
* Fixed a network encode error in ItemAdapter.CODEC.

## 7.1.6
* More codec things.

## 7.1.5
* Changed StepFunction.CODEC to accept constant float values as well as the usual definition.
* Added the GetEnchantmentLevelEvent, which allows changing enchantment levels for gameplay purposes.
* Fixed ItemUseEvent not accepting null players as specified by UseOnContext.

## 7.1.4
* Added a hook for Placebo Container Menus to react to successful shift-click operations.

## 7.1.3
* Added Codec support to PSerializer.

## 7.1.2
* Fixed Optional-related crashes.

## 7.1.1
* Added the ability to access the max and current steps of a StepFunction.
* StepFunction is now a final class.

## 7.1.0
* Removed DimWeightedJsonReloadListener in favor of better logic in WeightedJsonReloadListener.
* Changed .cfg files such that they preserve insertion order for entries.

## 7.0.1
* Added everything needed to port Apotheosis.

## 7.0.0
* Updated to 1.19.2

## 6.6.6
* Added some additional json utility code.

## 6.6.5
* Added a variant of blit() that takes a color arg to ClientUtil.
* Fixed the rainbow color breaking on servers.
* Added boilerplate for TOP entity additions.
* Added strict error logging to RecipeHelper.
* Made RecipeHelper accept TagKey as an input type.
* Added more setters to ModifiableEnergyStorage.

## 6.6.4
* Added the AnvilLandEvent.
* Added a very angry logging message if a TickingEntityBlock has the wrong entity type class.

## 6.6.3
* Fixed a crash when a dimensional reload listener has no items for that dimension.

## 6.6.2
* Added DimWeightedJsonReloadListener for dimension-restricted weighted items.
* Pulled in StepFunction from Apotheosis.

## 6.6.1
* Fixed deserialized StackLootEntries not passing in conditions or functions.

## 6.6.0
* Added a sketchy reflection-based auto-registration method for SerializerBuilder.
  * It coerces read(FriendlyByteBuf), read(JsonObject), write(FriendlyByteBuf) and JsonObject write() from the source class as a serializer.

## 6.5.3
* Added WeightedJsonReloadListener to better handle weighted entries.

## 6.5.2
* Re-added the ItemUseEvent needed by Apotheosis.
  * Forge declined to add this event, so here it will stay.

## 6.5.1
* Made RandomAttributeModifier not crash on fail due to how it is used in Gateways.
  * Fixes GatewaysToEternity#3

## 6.5.0
* Rewrite to RecipeHelper to allow recipes to be regenerated on reload, rather than built once.
  * This means that configs that adjust recipes can work without restarting the game.
  * However, due to timing, it is likely they will need a double-reload to trigger.
* Added various utilities needed by Gateways to Eternity 2.0.0.
* Fixed a bug where Placebo-JSON reload listeners were syncing data back to the host on SSP/LAN
  * This caused data to be lost when only part of the object is sent to the client for any reason.

## 6.4.2
* Made changes to how energy syncs in Placebo Menus.
  * This should fix energy values going negative, I hope...

## 6.4.1
* Fixed recipe types not being registered.

## 6.4.0
* Minimum Forge Version bumped to 40.1.14.
  * Needed to access IContext from AddReloadListenerEvent
* Fixed PlaceboJsonReloadListener(s) failing to sync on the first login to a server.
  * This change is breaking, and mods using PJRL from 6.3.0 will not work with this build.
  * All impacted mods will have corresponding updates.
* Added new functionality to Configuration to support top-level comments, as well as a title.
  * A document detailing the file specification will also be linked to.
* Made QuickMoveHandler call setChanged on the clicked slot for shift-clicks.

## 6.3.0
* Fixed PlaceboJsonReloadListeners not checking IConditions.

## 6.2.1
* Fixed RecipeHelper not being invoked.

## 6.2.0
* Updated to 1.18.2 - This was a breaking change due to tag updates within MC.
* Fixed Rainbow Color.
* Added a new utility to register RecipeTypes since the old method is defunct.

## 6.1.1
* Added support for loading conditions on ALL placebo-based json reload listeners.
  * This includes Hostile Neural Networks, Apotheosis, and any other future projects.

## 6.1.0
* Added the PlaceboJsonReloadListener, which assists in creation of automatically synced data-driven systems.
* Rewrote EasyContainerData and PlaceboContainerMenu to allow data slots to have separate real and synced values.
  * Breaking Change
* Added the base code for TOP compat.

## 6.0.5
* Added util to check if LCTRL or RCTRL is down.

## 6.0.4
* Sushomeister: Added Ukranian and Russian translations.
* Hav3funDJ: Added Italian translation.
* Added util to add lore to an itemstack.
* Added the PlaceboTaskQueue to enqueue tick-based tasks.

## 6.0.3
* Added the ability to handle optional stacks in ItemAdapter.

## 6.0.2
* Fixed a bug where mods using QuickMoveHandler were crashing outside of dev.

## 6.0.1
* Fully updated to 1.18.1

## 6.0.0
* Barebones port of what is necessary for PackMenu (The config system).

## 5.0.3
* Added util to add the commonly-used inventory shuffle movement rules.

## 5.0.2
* Added a ton more container util code.

## 5.0.1
* Full port to 1.17.1
* Added some useful Container, TileEntity, and GUI Utils.
* Moved most JS Coremods to Mixin where possible.
* Stripped new events and PR'd to forge.
* Removed deprecated code.

## 5.0.0
* Barebones port of what is necessary for PackMenu (The config system).

## 4.6.1
* Placebo loot tables will now properly have their ID set.
* Added a /placebo command
* Added a subcommand to allow for serialization of existing loot tables.
* Cleaned up some code that needed.

## 4.6.0
* Code has been updated to Official (Mojang) Mappings.
* TagBuilder and ReflectionHelper have been deprecated and are pending removal.