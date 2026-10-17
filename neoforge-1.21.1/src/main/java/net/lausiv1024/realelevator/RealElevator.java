package net.lausiv1024.realelevator;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.lausiv1024.realelevator.command.DebugCommands;
import net.lausiv1024.realelevator.client.ClientAnnouncements;
import net.lausiv1024.realelevator.network.AnnouncePayload;
import net.lausiv1024.realelevator.network.PressButtonPayload;
import net.lausiv1024.realelevator.network.PanelPayloads;
import net.lausiv1024.realelevator.registry.ModBlockEntities;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.lausiv1024.realelevator.registry.ModDataComponents;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.lausiv1024.realelevator.registry.ModItems;
import net.lausiv1024.realelevator.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
        modEventBus.addListener(RealElevator::registerPayloads);
        modEventBus.addListener(RealElevator::addCreativeTabItems);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DebugCommands.register(event.getDispatcher()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(PressButtonPayload.TYPE, PressButtonPayload.STREAM_CODEC, PressButtonPayload::handle)
                .playToServer(PanelPayloads.Settings.TYPE, PanelPayloads.Settings.STREAM_CODEC, PanelPayloads.Settings::handle)
                .playToServer(PanelPayloads.Install.TYPE, PanelPayloads.Install.STREAM_CODEC, PanelPayloads.Install::handle)
                // クライアント側の処理は、ラムダの中で呼ぶのでサーバーでは読み込まれない
                .playToClient(AnnouncePayload.TYPE, AnnouncePayload.STREAM_CODEC, (payload, context) -> ClientAnnouncements.handle(payload));
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.CONTROL_PANEL);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.FLOOR_MARKER);
        }
    }
}
