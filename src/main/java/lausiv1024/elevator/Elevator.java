package lausiv1024.elevator;

import net.minecraft.world.World;

import java.util.UUID;

public class Elevator extends AbstractElevator {
    public Elevator(int index, UUID elevatorUUID, World world) {
        super(index, elevatorUUID, world);
    }

    @Override
    public void setDoorMotion(double speed) {

    }
}
