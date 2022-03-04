package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.elevator.Elevator;
import net.minecraft.tileentity.TileEntityType;

import java.util.UUID;

public class ElevatorControllerTE extends ElevatorPartTE{
    Elevator elevator;

    public ElevatorControllerTE(TileEntityType<?> tileEntityType, UUID elevatorID) {
        super(tileEntityType, elevatorID);
        isController = true;
    }

    public ElevatorControllerTE(){
        super(RETileEntities.ELEVATOR_CONTROLLER.get());
        elevator = null;
    }

    public ElevatorControllerTE(Elevator elevator){
        super(RETileEntities.ELEVATOR_CONTROLLER.get());
        this.elevator = elevator;
    }
}
