package net.lausiv1024.realelevator;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.lausiv1024.realelevator.command.DebugCommands;
import net.lausiv1024.realelevator.client.ClientAnnouncements;
import net.lausiv1024.realelevator.client.ClientPresets;
import net.lausiv1024.realelevator.client.RenewalScreen;
import net.lausiv1024.realelevator.network.AnnouncePayload;
import net.lausiv1024.realelevator.network.PressButtonPayload;
import net.lausiv1024.realelevator.network.PanelPayloads;
import net.lausiv1024.realelevator.network.PowerPayloads;
import net.lausiv1024.realelevator.network.PresetPayloads;
import net.lausiv1024.realelevator.network.ProfilePayloads;
import net.lausiv1024.realelevator.network.RenewalPayloads;
import net.lausiv1024.realelevator.registry.ModBlockEntities;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.lausiv1024.realelevator.registry.ModDataComponents;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.lausiv1024.realelevator.registry.ModItems;
import net.lausiv1024.realelevator.registry.ModMenus;
import net.lausiv1024.realelevator.registry.ModRecipes;
import net.lausiv1024.realelevator.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RealElevator.MODID)
public class RealElevator {
    public static final String MODID = "realelevator";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealElevator(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModMenus.MENU_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(RealElevator::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.SERVER, RealElevatorConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, RealElevatorClientConfig.SPEC);
        modEventBus.addListener(RealElevator::addCreativeTabItems);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DebugCommands.register(event.getDispatcher()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(PressButtonPayload.TYPE, PressButtonPayload.STREAM_CODEC, PressButtonPayload::handle)
                .playToServer(PanelPayloads.Settings.TYPE, PanelPayloads.Settings.STREAM_CODEC, PanelPayloads.Settings::handle)
                .playToServer(PanelPayloads.Install.TYPE, PanelPayloads.Install.STREAM_CODEC, PanelPayloads.Install::handle)
                .playToServer(RenewalPayloads.Open.TYPE, RenewalPayloads.Open.STREAM_CODEC, RenewalPayloads.Open::handle)
                .playToServer(RenewalPayloads.Apply.TYPE, RenewalPayloads.Apply.STREAM_CODEC, RenewalPayloads.Apply::handle)
                .playToServer(RenewalPayloads.Dismantle.TYPE, RenewalPayloads.Dismantle.STREAM_CODEC, RenewalPayloads.Dismantle::handle)
                .playToServer(RenewalPayloads.Group.TYPE, RenewalPayloads.Group.STREAM_CODEC, RenewalPayloads.Group::handle)
                .playToServer(PowerPayloads.Select.TYPE, PowerPayloads.Select.STREAM_CODEC, PowerPayloads.Select::handle)
                .playToServer(ProfilePayloads.UseOnDoor.TYPE, ProfilePayloads.UseOnDoor.STREAM_CODEC, ProfilePayloads.UseOnDoor::handle)
                .playToServer(ProfilePayloads.Write.TYPE, ProfilePayloads.Write.STREAM_CODEC, ProfilePayloads.Write::handle)
                // クライアント側の処理は、ラムダの中で呼ぶのでサーバーでは読み込まれない
                .playToClient(AnnouncePayload.TYPE, AnnouncePayload.STREAM_CODEC, (payload, context) -> ClientAnnouncements.handle(payload))
                .playToClient(RenewalPayloads.Show.TYPE, RenewalPayloads.Show.STREAM_CODEC, (payload, context) -> RenewalScreen.open(payload))
                .playToClient(PresetPayloads.Sync.TYPE, PresetPayloads.Sync.STREAM_CODEC, (payload, context) -> ClientPresets.handle(payload));
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.INSTALLER);
            event.accept(ModItems.POWER_RECEIVER);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            ModItems.MATERIALS.forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.FLOOR_MARKER);
            event.accept(ModItems.MAINTENANCE_WRENCH);
            event.accept(ModItems.SPECIFICATION);
        }
    }
}
