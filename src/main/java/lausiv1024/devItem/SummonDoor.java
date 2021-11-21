package lausiv1024.devItem;

import lausiv1024.REBlocks;
import lausiv1024.blocks.DoorRail;
import lausiv1024.entity.doors.ElevatorDoorNoWindow;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SummonDoor extends DebugItem{
    public SummonDoor(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        BlockPos clickedPos = context.getClickedPos().above();
        PlayerEntity player = context.getPlayer();
        World level = context.getLevel();
        BlockState clickedState = level.getBlockState(clickedPos);
        Direction clickedFace = context.getClickedFace();

        if (clickedFace == Direction.UP){
            if (clickedState.getBlock() == REBlocks.DOOR_THRESHOLD.get()){
                Direction blockDirection = clickedState.getValue(DoorRail.FACING);
                ElevatorDoorNoWindow elevatorDoorNoWindow = new ElevatorDoorNoWindow(level, blockDirection.getOpposite());
                elevatorDoorNoWindow.setPos(clickedPos.getX() + 0.5, clickedPos.getY(), clickedPos.getZ());
                level.addFreshEntity(elevatorDoorNoWindow);
                return ActionResultType.SUCCESS;
            }
        }

        return ActionResultType.FAIL;
    }
}
