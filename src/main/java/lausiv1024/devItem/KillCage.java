package lausiv1024.devItem;

import lausiv1024.REBlocks;
import lausiv1024.blocks.DoorRail;
import lausiv1024.entity.CageEntity;
import lausiv1024.entity.EleButtonEntity;
import lausiv1024.entity.ElevatorPartEntity;
import lausiv1024.entity.doors.ElevatorDoorNoWindow;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Predicate;

public class KillCage extends DebugItem{
    public KillCage(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity entity, Hand hand) {
        Vector3d plPos = entity.position();
        AxisAlignedBB bb = new AxisAlignedBB(plPos.x - 4, plPos.y - 4, plPos.z - 4,
                plPos.x + 4, plPos.y + 4, plPos.z + 4);
        List<ElevatorPartEntity> e = world.getEntitiesOfClass(ElevatorPartEntity.class, bb,
                entity1 -> (entity1 instanceof CageEntity || entity1 instanceof EleButtonEntity));
        e.forEach(en -> en.remove());
        world.playSound(entity, plPos.x, plPos.y, plPos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundCategory.MASTER, 1.0f,
                1.0f);
        return ActionResult.success(entity.getItemInHand(hand));
    }
}
