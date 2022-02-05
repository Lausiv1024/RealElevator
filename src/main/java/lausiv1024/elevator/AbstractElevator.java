package lausiv1024.elevator;

public abstract class AbstractElevator {
    protected String[] displayName;
    protected int[] floorAnnounceId;
    protected int floorCount;
    protected int curFloor = 0;
    protected int doorState = 0;
    protected int doorCloseTick = 60;
    protected int announceTick = 0;
    protected boolean forceClosing = false;
    protected boolean hasAnnounce = true;

    public AbstractElevator(int index){
        floorCount = index;
        displayName = new String[index];
        floorAnnounceId = new int[index];
    }

    public void elevatorTick(){
        doorMotion();
        if (announceTick > 0){
            announceTick--;
        }
    }

    public abstract void doorMotion();

    public void openButtonPressed(){
        if (doorState == 3){
            doorState = 4;
        }
        resetDoorCloseTick(false);
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
