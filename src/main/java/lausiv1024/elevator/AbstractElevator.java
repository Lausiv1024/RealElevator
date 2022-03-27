package lausiv1024.elevator;

import com.google.common.collect.Maps;
import lausiv1024.RealElevator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;

public abstract class AbstractElevator {
    //エレベーターの参照データ Map < elevatorId, offsetPos>
    private static final Map<UUID, BlockPos> ELEVATOROFFS = Maps.newHashMap();

    private static final Logger LOGGER = LogManager.getLogger("ElevatorClass");


    public UUID elevatorID;
    protected String[] displayName;
    protected boolean[] registered;
    protected int floorCount;
    protected int curFloor = 0;
    protected byte doorState = 0;
    protected int doorCloseTick = 60;
    protected int openTime = 0;
    protected boolean forceClosing = false;
    protected boolean hasAnnounce = false;
    protected ElevatorDirection direction;
    protected Direction facingMain = Direction.NORTH;
    protected int going = -1;
    protected EnumCallingState[] called;
    protected int[] floorYPos;
    protected boolean moving = false;
    protected boolean initialized = false;
    protected ElevatorObjectReference ref;
    protected DoorManager doorMgr;
    protected BlockPos offSet;

    public AbstractElevator(int index, UUID elevatorID, BlockPos offSet, String[] dispName, int[] yPoses){
        floorCount = index;
        displayName = dispName;
        registered = new boolean[index];
        called = new EnumCallingState[index];
        floorYPos = yPoses;
        direction = ElevatorDirection.NONE;
        this.elevatorID = elevatorID;
        this.offSet = offSet;
        ref = new ElevatorObjectReference();
        ELEVATOROFFS.put(elevatorID, offSet);
    }

    public void elevatorTick(World world){
        if (!initialized){
            init();
            return;
        }

         if (doorState == 0){
            elevatorMainTick();
            forceClosing = false;
            openTime = 0;
            resetDoorCloseTick(true);
        }else if (doorState == 2){
            if (!checkSafety(true)) resetDoorCloseTick(false);
            if (!forceClosing)
                //openTime++;
            if (openTime == 600)
                forceClosing = true;
        }else if (doorState == 3){
            if (!checkSafety(true)) {
                resetDoorCloseTick(false);
                doorState = 4;
            }
        }

        //doorMgr.doorTick(this);
    }

    protected void init(){

        initialized = true;
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

    public void setDoorState(byte doorState) {
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

    public ElevatorObjectReference getRef() {
        return ref;
    }

    private int getTopPos(){
        if (floorYPos.length < 2){
            RealElevator.LOGGER.warn("Invalid Index!!");
            //1.18に対応させることを考えて
            return -65;
        }
        return floorYPos[floorYPos.length - 1] + 3;
    }

    public Direction getFacingMain() {
        return facingMain;
    }

    public void setFacingMain(Direction facingMain) {
        this.facingMain = facingMain;
    }

    private int getBottomPos(){
        return floorYPos[0] - 2;
    }

    public CompoundNBT saveToNbt(){
        CompoundNBT parent = new CompoundNBT();
        //Write ElevatorID, Cage UUID, CWT UUID
        //CWT is Counterweight
        if (elevatorID != null) parent.putUUID("ElevatorID", elevatorID);
//        ObjHelper.checkNullAndExec(ref.cage, () -> parent.putUUID("CageId", ref.cage));
//        ObjHelper.checkNullAndExec(ref.cwt, () -> parent.putUUID("CwtId", ref.cwt));
        if (ref != null){
            if (ref.cage != null) parent.putUUID("CageId", ref.cage);
            if (ref.cwt != null) parent.putUUID("CwtId", ref.cwt);
        }


        ListNBT flInfoList = new ListNBT();
        for (int i = 0; i < floorCount; i++){
            //final int i2 = i;
            CompoundNBT fData = new CompoundNBT();
            fData.putInt("Ypos", floorYPos[i]);
            fData.putString("DispName", displayName[i]);
            fData.putBoolean("Registered", registered[i]);
            //ObjHelper.checkNullAndExec(called[i2], () -> fData.putString("CallState", called[i2].name()));
            if (called[i] != null) fData.putString("CallState", called[i].name());
            flInfoList.add(fData);
        }
        parent.put("FloorInfo", flInfoList);
        parent.putInt("CurrentFloor", curFloor);
        parent.putByte("DoorState", doorState);
        parent.putInt("DoorCloseTick", doorCloseTick);
        //強制戸閉機能どうしよう
//        parent.putInt("OpenTime", openTime);
//        parent.putBoolean("ForceClosing", forceClosing);
        parent.putString("ElevatorDirection", direction.name());
        parent.putString("Facing", facingMain.name());

        parent.putInt("GoingFloor", going);
        parent.putBoolean("Moving", moving);

        if (offSet != null)
            parent.put("Offset", NBTUtil.writeBlockPos(offSet));

        return parent;
    }

    public static Elevator fromNBT(CompoundNBT nbt){
        final UUID elevatorId = nbt.getUUID("ElevatorID");

        final ListNBT floorInfo = nbt.getList("FloorInfo", 10);
        int length = floorInfo.size();
        int[] yPos = new int[length];
        String[] dispName = new String[length];
        boolean[] registered = new boolean[length];
        EnumCallingState[] callState = new EnumCallingState[length];
        for (int i = 0; i < length; i++){
            CompoundNBT nbt1 = floorInfo.getCompound(i);
            yPos[i] = nbt1.getInt("Ypos");
            dispName[i] = nbt1.getString("DispName");
            registered[i] = nbt1.getBoolean("Registered");
            try {
                callState[i] = EnumCallingState.valueOf(nbt1.getString("CallState"));
            }catch (IllegalArgumentException e){
                callState[i] = EnumCallingState.NONE;
            }
        }

        BlockPos offs = NBTUtil.readBlockPos(nbt.getCompound("Offset"));

        Elevator elevator = new Elevator(length, elevatorId, offs, dispName, yPos);

        ElevatorObjectReference reference = new ElevatorObjectReference();
        reference.cage = nbt.getUUID("CageId");
        reference.cwt = nbt.getUUID("CwtId");
        elevator.ref = reference;

        elevator.registered = registered;
        elevator.called = callState;
        elevator.curFloor = nbt.getInt("CurrentFloor");
        elevator.doorState = nbt.getByte("DoorState");
        elevator.doorCloseTick = nbt.getInt("DoorCloseTick");
        elevator.going = nbt.getInt("GoingFloor");
        elevator.moving = nbt.getBoolean("Moving");

        try {
            elevator.direction = ElevatorDirection.valueOf(nbt.getString("ElevatorDirection"));
            elevator.facingMain = Direction.valueOf(nbt.getString("Facing"));
        }catch (IllegalArgumentException e){
            LOGGER.info("Direction or ElevatorFacing is empty");
        }

        return elevator;
    }
    public static BlockPos getElevatorOff(UUID elevatorID){
        return ELEVATOROFFS.get(elevatorID);
    }
}
