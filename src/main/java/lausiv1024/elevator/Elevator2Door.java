package lausiv1024.elevator;

import net.minecraft.world.World;

import java.util.UUID;

public class Elevator2Door extends AbstractElevator{
    public int[] doorDirection;

    public Elevator2Door(int index, UUID elevatorId, World world) {
        super(index, elevatorId, world);
        doorDirection =new int[index];
    }

    @Override
    public void setDoorMotion(double speed) {

    }
}
