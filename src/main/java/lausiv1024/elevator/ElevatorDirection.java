package lausiv1024.elevator;

public enum ElevatorDirection {
    NONE(0),
    UP(1),
    DOWN(2);
    public final int nbt_index;
    ElevatorDirection(int a){
        nbt_index = a;
    }

    public static ElevatorDirection getElevatorDirectionFromIndex(int nbt_index){
        return ElevatorDirection.values()[nbt_index];
    }
}
