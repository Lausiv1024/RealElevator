package lausiv1024.elevator;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class Elevator extends AbstractElevator {

    public Elevator(int index, UUID elevatorID, BlockPos offSet, String[] dispName, int[] yPoses) {
        super(index, elevatorID, offSet, dispName, yPoses);
    }

    @Override
    public void setDoorMotion(double speed) {

    }
}
