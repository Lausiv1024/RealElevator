package lausiv1024.devItem;

import lausiv1024.RealElevator;
import lausiv1024.elevator.AbstractElevator;
import lausiv1024.entity.CageEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.List;

public class GetCagePos extends DebugItem{
    public GetCagePos(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        Vector3d vec = playerEntity.position();
        AxisAlignedBB bb = new AxisAlignedBB(vec.x - 5, vec.y - 5, vec.z - 5,
                vec.x + 5, vec.y + 5, vec.z + 5);
        world.playSound(playerEntity, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
        if (world.isClientSide) return ActionResult.success(playerEntity.getItemInHand(hand));
        List<CageEntity> cages = world.getEntitiesOfClass(CageEntity.class, bb, entity -> true);
        cages.forEach(cage -> {
            Vector3d cagePos = cage.position();
            BlockPos offs = AbstractElevator.getElevatorOff(cage.getElevatorId());
//            RealElevator.LOGGER.info("CageGlobalPOS > [{}] Offset > [{}]   LocalPos > [{}]",
//                    cagePos, offs, new Vector3d(cagePos.x - offs.getX(), cagePos.y - offs.getY(), cagePos.z - offs.getZ()));
            RealElevator.LOGGER.info(cagePos);
        });
        return ActionResult.success(playerEntity.getItemInHand(hand));
    }
}
