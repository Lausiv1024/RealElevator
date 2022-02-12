package lausiv1024.elevator;

public enum EnumCallingState {
    UP,
    DOWN,
    BOTH,
    NONE;
    public static ElevatorDirection convert(EnumCallingState direction){
        switch (direction){
            case UP:
                return ElevatorDirection.UP;
            case DOWN:
                return ElevatorDirection.DOWN;
            default:
                return ElevatorDirection.NONE;
        }
    }
}
