package lausiv1024.elevator;

import java.util.UUID;

public class Elevator extends AbstractElevator {
    public Elevator(int index, UUID elevatorUUID) {
        super(index, elevatorUUID);
    }

    @Override
    public void setDoorMotion(double speed) {

    }
}
