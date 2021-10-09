package lausiv1024;

import lausiv1024.devItem.REDevItems;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RealElevatorCore.ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RealElevatorCore {
    public static final String ID = "realelevator";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public RealElevatorCore(){
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        REEntities.register(eventBus);
        REBlocks.register(eventBus);
        REItems.register(eventBus);
        REDevItems.register(eventBus);
        RETileEntities.register(eventBus);
    }

    public static final ItemGroup REAL_ELEVATOR_GROUP = new ItemGroup("real_elevator") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(REItems.FLOOR_MARKER.get());
        }
    };

    @SubscribeEvent
    public static void loadComplete(FMLLoadCompleteEvent event){
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void modelRegistry(ModelRegistryEvent event){
        LOGGER.info("ClientSetUp");
//        RenderingRegistry.registerEntityRenderingHandler(REEntities.ELEVATOR_BUTTON,
//                EleButtonRenderer::new);
    }
}
