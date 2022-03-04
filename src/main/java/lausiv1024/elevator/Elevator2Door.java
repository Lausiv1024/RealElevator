package lausiv1024.elevator;

import java.util.UUID;

public class Elevator2Door extends AbstractElevator{
    public int[] doorDirection;

    public Elevator2Door(int index, UUID elevatorId) {
        super(index, elevatorId);
        doorDirection =new int[index];
    }

    @Override
    public void setDoorMotion(double speed) {

    }
}
