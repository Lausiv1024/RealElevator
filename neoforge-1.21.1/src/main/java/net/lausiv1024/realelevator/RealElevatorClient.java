package net.lausiv1024.realelevator;

import net.lausiv1024.realelevator.client.ControlPanelRenderer;
import net.lausiv1024.realelevator.client.ElevatorCarRenderer;
import net.lausiv1024.realelevator.client.LandingDoorRenderer;
import net.lausiv1024.realelevator.registry.ModBlockEntities;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = RealElevator.MODID, dist = Dist.CLIENT)
public class RealElevatorClient {
    public RealElevatorClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(RealElevatorClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ELEVATOR_CAR.get(), ElevatorCarRenderer::new);
        event.registerEntityRenderer(ModEntities.LANDING_DOOR.get(), LandingDoorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CONTROL_PANEL.get(), ControlPanelRenderer::new);
    }
}
