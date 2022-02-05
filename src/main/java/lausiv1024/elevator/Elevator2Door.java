package lausiv1024.elevator;

public class Elevator2Door extends AbstractElevator{
    public int[] doorDirection;

    public Elevator2Door(int index) {
        super(index);
        doorDirection =new int[index];
    }

    @Override
    public void setDoorMotion(double speed) {

    }
}
