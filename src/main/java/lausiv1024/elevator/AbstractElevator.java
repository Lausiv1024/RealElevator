package lausiv1024.elevator;

import net.minecraft.util.Direction;

public abstract class AbstractElevator {
    protected String[] displayName;
    protected int[] floorAnnounceId;
    protected boolean[] registered;
    protected int floorCount;
    protected int curFloor = 0;
    protected int doorState = 0;
    protected int doorCloseTick = 60;
    protected int openTime = 0;
    protected int announceTick = 0;
    protected boolean forceClosing = false;
    protected boolean hasAnnounce = true;
    protected final int doorCount;
    protected ElevatorDirection direction;
    protected ElevatorEntityHolder eleObj;
    protected Direction facingMain;
    protected int going = -1;

    public AbstractElevator(int index){
        floorCount = index;
        displayName = new String[index];
        floorAnnounceId = new int[index];
        registered = new boolean[index];
        doorCount = 2;
        direction = ElevatorDirection.NONE;
    }

    public void elevatorTick(){
        doorMotion();
        if (announceTick > 0){
            announceTick--;
        }
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
            announceTick = -1;
            announce(AnnounceType.DOOR_CLOSE);
            resetDoorCloseTick(false);
            doorState = 3;
        }
    }

    public void elevatorMainTick(){
        if (going == -1) setGoingFloor();
    }

    private void setGoingFloor(){
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
        }
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

    public boolean isForceClosing() {
        return forceClosing;
    }

    public void setHasAnnounce(boolean hasAnnounce) {
        this.hasAnnounce = hasAnnounce;
    }

    public void updateFloor(int index, String[] displayName, int[] floorAnnounceId){
        this.floorAnnounceId = floorAnnounceId;
        this.displayName = displayName;
        this.floorCount = index;
    }
}
