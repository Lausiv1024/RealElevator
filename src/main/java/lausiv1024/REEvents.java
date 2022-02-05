package lausiv1024;

import lausiv1024.blocks.ElevatorPartBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class REEvents {
//    @SubscribeEvent
//    public static void event2(BlockEvent.BreakEvent event){
//        BlockPos pos = event.getPos();
//        IWorld world = event.getWorld();
//        if (world.getBlockState(pos).getBlock() instanceof ElevatorPartBlock){
//            event.setCanceled(true);
//        }
//    }

    public static void en(PlayerInteractEvent.EntityInteract event){
        //event.getTarget().setDeltaMovement(event.getTarget().getDeltaMovement().add(0.0, 0.4, 0.0));
    }
}
