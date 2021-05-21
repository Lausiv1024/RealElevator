package lausiv1024;

import lausiv1024.tileentity.render.HoleLanternRender;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientProxy {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event){
        //ClientRegistry.bindTileEntityRenderer(RETileEntities.HOLE_LANTERN, HoleLanternRender::new);
        RealElevatorCore.LOGGER.info("ClientSetUp");
    }
}
