package lausiv1024.elevator;

public class Elevator extends AbstractElevator {
    public Elevator(int index) {
        super(index);
    }

    @Override
    public void doorMotion() {
        if (doorState == 2){
            doorCloseTick++;
            if (doorCloseTick == 0){
                doorState = 3;
            }
        }
    }
}
