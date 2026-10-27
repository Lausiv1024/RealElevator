package net.lausiv1024.realelevator;

import net.lausiv1024.realelevator.client.InstallerRenderer;
import net.lausiv1024.realelevator.client.InstallerScreen;
import net.lausiv1024.realelevator.client.ElevatorCarRenderer;
import net.lausiv1024.realelevator.client.LandingDoorRenderer;
import net.lausiv1024.realelevator.client.VoicevoxScreen;
import net.lausiv1024.realelevator.registry.ModBlockEntities;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.lausiv1024.realelevator.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = RealElevator.MODID, dist = Dist.CLIENT)
public class RealElevatorClient {
    public RealElevatorClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(RealElevatorClient::registerRenderers);
        modEventBus.addListener((RegisterMenuScreensEvent event) -> event.register(ModMenus.INSTALLER.get(), InstallerScreen::new));
        // モッド一覧の「設定」から、VOICEVOX 連携の設定画面を開く
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new VoicevoxScreen(parent));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ELEVATOR_CAR.get(), ElevatorCarRenderer::new);
        event.registerEntityRenderer(ModEntities.LANDING_DOOR.get(), LandingDoorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INSTALLER.get(), InstallerRenderer::new);
    }
}
