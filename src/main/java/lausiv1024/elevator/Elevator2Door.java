package lausiv1024.elevator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class Elevator2Door extends AbstractElevator{
    public int[] doorDirection;

    public Elevator2Door(int index, UUID elevatorID, BlockPos offSet, String[] dispName, int[] yPoses) {
        super(index, elevatorID, offSet, dispName, yPoses);
        doorDirection = new int[index];
    }


    @Override
    public void setDoorMotion(double speed) {

    }
}
