package lausiv1024.client;

import lausiv1024.REBlocks;
import lausiv1024.REEntities;
import lausiv1024.RETileEntities;
import lausiv1024.client.render.entity.CageRenderer;
import lausiv1024.client.render.entity.CwtEntityRenderer;
import lausiv1024.client.render.entity.DoorNoWindowRenderer;
import lausiv1024.client.render.entity.EleButtonRenderer;
import lausiv1024.client.render.tileentity.FlControllerRenderer;
import lausiv1024.client.render.tileentity.FloorDisplayRenderer;
import lausiv1024.client.render.tileentity.HoleLanternRender;
import lausiv1024.client.render.tileentity.LandingButtonTERenderer;
import lausiv1024.entity.CwtEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ModelLoaderRegistry;
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
        ClientRegistry.bindTileEntityRenderer(RETileEntities.HOLE_LANTERN.get(), arg -> new HoleLanternRender<>(TileEntityRendererDispatcher.instance));
        ClientRegistry.bindTileEntityRenderer(RETileEntities.FLOOR_DISPLAY.get(), arg -> new FloorDisplayRenderer<>(TileEntityRendererDispatcher.instance));
        ClientRegistry.bindTileEntityRenderer(RETileEntities.LANDING_BUTTON_TE.get(), arg -> new LandingButtonTERenderer<>(TileEntityRendererDispatcher.instance));
        ClientRegistry.bindTileEntityRenderer(RETileEntities.FLOOR_CONTROLLER_TE.get(), arg -> new FlControllerRenderer<>(TileEntityRendererDispatcher.instance));

        RenderingRegistry.registerEntityRenderingHandler(REEntities.ELEVATOR_BUTTON.get(), EleButtonRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), DoorNoWindowRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(REEntities.CWT.get(), CwtEntityRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(REEntities.CAGE.get(), CageRenderer::new);

        setBlockRenderType();
    }

    private static void setBlockRenderType(){
        RenderType cutout = RenderType.cutout();
        RenderTypeLookup.setRenderLayer(REBlocks.EDNW.get(), cutout);
    }
}
