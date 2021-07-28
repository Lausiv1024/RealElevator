package lausiv1024;

import lausiv1024.blocks.ElevatorPartBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class REEvents {
    @SubscribeEvent
    public static void destroyBlockEvent(LivingDestroyBlockEvent event){
    }

    @SubscribeEvent
    public static void event2(BlockEvent.BreakEvent event){
        BlockPos pos = event.getPos();
        IWorld world = event.getWorld();
        if (world.getBlockState(pos).getBlock() instanceof ElevatorPartBlock){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void event3(LivingEvent.LivingJumpEvent event){
//        LivingEntity entity = event.getEntityLiving();
//        if (entity instanceof PlayerEntity){
//            EffectInstance instance = new EffectInstance(Effects.JUMP, 40, 5, true, false);
//            entity.addEffect(instance);
//        }
    }

    @SubscribeEvent
    public static void event3(LivingEntityUseItemEvent event){
        RealElevatorCore.LOGGER.info("LivingEntityUse");
    }
}
