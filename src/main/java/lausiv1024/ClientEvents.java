package lausiv1024;

import lausiv1024.renderer.DoorNoWindowRenderer;
import lausiv1024.renderer.EleButtonRenderer;
import lausiv1024.tileentity.render.FloorDisplayRenderer;
import lausiv1024.tileentity.render.HoleLanternRender;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void setupClient(FMLClientSetupEvent event){
        ClientRegistry.bindTileEntityRenderer(RETileEntities.HOLE_LANTERN.get(),arg -> new HoleLanternRender(TileEntityRendererDispatcher.instance));
        ClientRegistry.bindTileEntityRenderer(RETileEntities.FLOOR_DISPLAY.get(), arg -> new FloorDisplayRenderer(TileEntityRendererDispatcher.instance));

        RenderingRegistry.registerEntityRenderingHandler(REEntities.ELEVATOR_BUTTON.get(), EleButtonRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), DoorNoWindowRenderer::new);

        setBlockRenderType();
    }

    private static void setBlockRenderType(){
        RenderType cutout = RenderType.cutout();
        RenderTypeLookup.setRenderLayer(REBlocks.GUIDE_RAILA.get(), cutout);
        RenderTypeLookup.setRenderLayer(REBlocks.GOMI.get(), cutout);
    }
}
