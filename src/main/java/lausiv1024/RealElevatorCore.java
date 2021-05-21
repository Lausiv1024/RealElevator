package lausiv1024;

import lausiv1024.tileentity.HoleLanternTile;
import lausiv1024.tileentity.render.ElevatorPartRender;
import lausiv1024.tileentity.render.HoleLanternRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.ChestTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RealElevatorCore.ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RealElevatorCore {
    public static final String ID = "realelevator";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void setupClient(FMLClientSetupEvent event){
        LOGGER.info("SetupClient");
        ClientRegistry.bindTileEntityRenderer(RETileEntities.HOLE_LANTERN,arg -> new HoleLanternRender(TileEntityRendererDispatcher.instance) {
        });
    }
}
