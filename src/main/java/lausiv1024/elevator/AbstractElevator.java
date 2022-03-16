package lausiv1024.elevator;

import net.minecraft.util.Direction;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class AbstractElevator {
    public final UUID elevatorID;
    protected String[] displayName;
    protected boolean[] registered;
    protected int floorCount;
    protected int curFloor = 0;
    protected int doorState = 0;
    protected int doorCloseTick = 60;
    protected int openTime = 0;
    protected boolean forceClosing = false;
    protected boolean hasAnnounce = false;
    protected ElevatorDirection direction;
    protected Direction facingMain;
    protected int going = -1;
    protected EnumCallingState[] called;
    protected int[] floorYPos;
    protected boolean moving = false;
    protected boolean initialized = false;
    protected UUID cwtId;
    protected UUID cageId;

    public AbstractElevator(int index, UUID elevatorID, World worldObj){
        floorCount = index;
        displayName = new String[index];
        registered = new boolean[index];
        called = new EnumCallingState[index];
        floorYPos = new int[index];
        direction = ElevatorDirection.NONE;
        this.elevatorID = elevatorID;
    }

    public void elevatorTick(){
        if (!initialized){
            init();
            return;
        }

        doorMotion();
         if (doorState == 0){
            elevatorMainTick();
            forceClosing = false;
            openTime = 0;
            resetDoorCloseTick(true);
        }else if (doorState == 2){
            if (!checkSafety(true)) resetDoorCloseTick(false);
            if (!forceClosing)
                openTime++;
            if (openTime == 600)
                forceClosing = true;
        }else if (doorState == 3){
            if (!checkSafety(true)) {
                resetDoorCloseTick(false);
                doorState = 4;
            }
        }
    }

    protected void init(){

        initialized = true;
    }

    public void doorMotion(){
        if (doorState == 2){
            doorCloseTick++;
            if (doorCloseTick == 0){
                doorState = 3;
            }
        }
    }

    private boolean checkSafety(boolean b){
        return false;
    }

    public void openButtonPressed(){
        if (doorState == 3){
            doorState = 4;
        }
        resetDoorCloseTick(false);
    }

    public void closeButtonPressed(){
        if (doorState == 2){
            doorCloseTick = 0;
            announce(AnnounceType.DOOR_CLOSE);
            resetDoorCloseTick(false);
            doorState = 3;
        }
    }

    public void elevatorMainTick(){
    }

    public String[] getDisplayName() {
        return displayName;
    }

    //decideGoingFloor is called on door closed and called
    private void decideGoingFloor(){
        if (direction == ElevatorDirection.UP){
            for (int i = curFloor + 1; i < floorCount; i++){
                if (registered[i]){
                    going = i;
                    break;
                }
            }
        }else if (direction == ElevatorDirection.DOWN){
            for (int i = curFloor - 1; i >= 0; i--){
                if (registered[i]){
                    going = i;
                    break;
                }
            }
        }else{
            int lastI = -1;
            for (int i = 0; i < floorCount; i++){
                boolean reg = registered[i];
                if (reg){
                    boolean flag = false;
                    if (i < curFloor){
                        direction = ElevatorDirection.DOWN;
                        flag = true;
                    }else if (i > curFloor){
                        direction = ElevatorDirection.UP;
                        flag = true;
                    }
                    if (flag) lastI = i;
                }
            }
            going = lastI;
            if (going == -1) direction = ElevatorDirection.NONE;
        }
    }

    //setEleDirection is called on Call or Floor Button and arriving
    private void setEleDirection(){
        if (direction == ElevatorDirection.NONE){
            for (int i = 0; i < called.length;i++){
                EnumCallingState state = called[i];
                if (state == EnumCallingState.NONE) continue;
                if (curFloor == i){
                    direction = EnumCallingState.convert(state);
                    called[i] = EnumCallingState.NONE;
                    return;
                }

                if (curFloor < i){
                    direction = ElevatorDirection.UP;
                    return;
                }
                direction = ElevatorDirection.DOWN;
                return;
            }

            for (int j = 0; j < registered.length; j++){
                boolean reg = registered[j];
                if (!reg) continue;
                if (curFloor > j){
                    direction = ElevatorDirection.DOWN;
                }else if (curFloor < j){
                    direction = ElevatorDirection.UP;
                }
            }
        }
    }

    public boolean isMoving() {
        return moving;
    }

    public abstract void setDoorMotion(double speed);

    public void announce(AnnounceType type){

    }

    public void announce(int id){

    }

    private void lookUpRegisteredFloor(){

    }

    private void resetDoorCloseTick(boolean completelyReset){
        if (completelyReset){
            doorCloseTick = 60;
        }else{
            if (doorCloseTick >= 40){
                doorCloseTick = 60;
            }else{
                doorCloseTick = 40;
            }
        }
    }

    public int getFloorCount() {
        return floorCount;
    }

    public int getCurFloor() {
        return curFloor;
    }

    public int getDoorState() {
        return doorState;
    }

    public void setDoorState(int doorState) {
        if (doorState < 5)
            this.doorState = doorState;
    }

    public ElevatorDirection getDirection() {
        return direction;
    }

    public boolean isForceClosing() {
        return forceClosing;
    }

    public void setHasAnnounce(boolean hasAnnounce) {
        this.hasAnnounce = hasAnnounce;
    }

    public void updateFloor(int index, String[] displayName, int[] floorAnnounceId){
        this.displayName = displayName;
        this.floorCount = index;
    }
}
