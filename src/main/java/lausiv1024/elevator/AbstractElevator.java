package lausiv1024.elevator;

import com.google.common.collect.Maps;
import lausiv1024.RealElevator;
import lausiv1024.entity.CageEntity;
import lausiv1024.entity.EleButtonEntity;
import lausiv1024.entity.ElevatorPartEntity;
import lausiv1024.entity.doors.AbstractDoorEntity;
import lausiv1024.tileentity.ElevatorControllerTE;
import lausiv1024.tileentity.LandingButtonBlockTE;
import lausiv1024.util.RotatableBoxShape;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractElevator {
    //エレベーターの参照データ Map < elevatorId, offsetPos>
    private static final Map<UUID, BlockPos> ELEVATOROFFS = Maps.newHashMap();

    private static final Logger LOGGER = LogManager.getLogger("ElevatorClass");


    public UUID elevatorID;
    protected String[] displayName;
    protected boolean[] registered;
    protected NonNullList<Boolean> isEnabled;//falseで不停止設定
    protected int floorCount;
    protected int curFloor = 0;
    protected byte doorState = 0; //0:Closed  1:Opening  2:Opened  3:Closing  4:Inverting
    protected int doorCloseTick = 80;
    protected int openTime = 0;
    protected boolean forceClosing = false;
    protected boolean hasAnnounce = false;
    protected ElevatorDirection direction;
    protected Direction facingMain = Direction.NORTH;
    protected int going = -1;
    protected NonNullList<EnumCallingState> called;
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
        isEnabled = NonNullList.withSize(index, true);
        called = NonNullList.withSize(index, EnumCallingState.NONE);
        floorYPos = yPoses;
        direction = ElevatorDirection.NONE;
        this.elevatorID = elevatorID;
        this.offSet = offSet;
        ref = new ElevatorObjectReference();
        ELEVATOROFFS.put(elevatorID, getControllerPos(offSet));
    }

    private BlockPos getControllerPos(BlockPos eleOffset){
        return new BlockPos(eleOffset.getX(), getTopPos() - 2, eleOffset.getZ() - 1);
    }

    public void unLoad(){
        ELEVATOROFFS.remove(elevatorID);
    }

    public void elevatorTick(World world){
        if (!initialized){
            init();
            return;
        }

         if (doorState == 0){
            elevatorMainTick(world);
            updateDispDirection(world);
            forceClosing = false;
            openTime = 0;
            resetDoorCloseTick(true);
        }else if (doorState == 2){

            if (!checkSafety(world)) {
                resetDoorCloseTick(false);
            }else doorCloseTick--;
//            if (!forceClosing)
//                openTime++;
//            if (openTime == 600)
//                forceClosing = true;

             if (doorCloseTick == 0){
                 setDoorClose(world);
             }
        }else if (doorState == 3){
            if (!checkSafety(world)) {
                resetDoorCloseTick(false);
                doorState = 1;
            }
        }

        //doorMgr.doorTick(this);
    }

    protected void init(){

        initialized = true;
    }


    private boolean checkSafety(World level){
        //ドアの間にエンティティがいないか確認
        //強制戸閉の場合、ドアにエンティティが接触するまでtrueを返し続ける
        //安全な状態ならtrueを返す
        CageEntity cage = (CageEntity) ElevatorPartEntity.getEntityFromUUID(ref.cage, level);
        if (cage == null || cage.doorMgr == null) return false;
        List<AxisAlignedBB> bbs = new ArrayList<>();
        if (cage.doorMgr.doorType == DoorManager.DoorType.CO){
            AbstractDoorEntity[] doors = cage.getDoors(true);
            for (AbstractDoorEntity door : doors){
                bbs.add(door.getBoundingBox());
            }
        }

        //正直AtomicBooleanはよくわからん
        AtomicBoolean flag = new AtomicBoolean(true);
        bbs.forEach(bbb -> {
            List<LivingEntity> collided = level.getEntitiesOfClass(LivingEntity.class, bbb, dsa -> true);
            //ドアへの接触が確認したらfalse
            if (collided.size() != 0) flag.set(false);
        });

        RotatableBoxShape boxShape = new RotatableBoxShape(-0.75, 0, 1.6875, 0.75, 2.0, 2.0025);
        AxisAlignedBB rotated = boxShape.toBB(facingMain);
        AxisAlignedBB moved = rotated.move(cage.position());
        List<LivingEntity> livings = level.getEntitiesOfClass(LivingEntity.class, moved, param -> true);
        if (livings.size() != 0) flag.set(false);

        return flag.get();
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

    private void elevatorMainTick(World world){
        if (!moving){
            decideGoingFloor();
        }
    }

    public String[] getDisplayName() {
        return displayName;
    }

    //decideGoingFloor is called on door closed and called
    private void decideGoingFloor(){
        if (direction == ElevatorDirection.UP){
            going = -1;
            for (int i = curFloor + 1; i < floorCount; i++){
                if (registered[i]){
                    going = i;
                    break;
                }
            }

            for (int ic = curFloor + 1; ic < floorCount; ic++){
                if (called.get(ic) == EnumCallingState.BOTH || called.get(ic) == EnumCallingState.UP){
                    if (going > -1 && going < ic){
                        break;
                    }
                    going = ic;
                    break;
                }
            }

            if (going == -1){
                direction = ElevatorDirection.NONE;
            }

        }else if (direction == ElevatorDirection.DOWN){
            going = -1;
            for (int i = curFloor - 1; i >= 0; i--){
                if (registered[i]){
                    going = i;
                    break;
                }
            }
            for (int i = curFloor - 1; i >= 0; i--){
                if (called.get(i) == EnumCallingState.BOTH || called.get(i) == EnumCallingState.DOWN){
                    if (going > i) break;
                    going = i;
                    break;
                }
            }
            if (going == -1) direction = ElevatorDirection.NONE;
        }else{//方向灯消灯時
            int lastI = -1;
            for (int i = 0; i < floorCount; i++){
                boolean reg = registered[i];
                EnumCallingState state = called.get(i);
                if (reg || state != EnumCallingState.NONE){
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

    //setEleDirection is called on Call or Floor Button and arrived
    private void setEleDirection(){
        if (direction == ElevatorDirection.NONE){
            for (int i = 0; i < called.size();i++){
                EnumCallingState state = called.get(i);
                if (state == EnumCallingState.NONE) continue;
                if (curFloor == i){
                    direction = EnumCallingState.convert(state);
                    called.set(i, EnumCallingState.NONE);
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
        }else{
            boolean flag = false;
            for (EnumCallingState state : called){
                if (state != EnumCallingState.NONE){
                    flag = true;
                    break;
                }
            }

            for (boolean reg : registered){
                if (reg){
                    flag = true;
                    break;
                }
            }
            if (!flag){
                direction = ElevatorDirection.NONE;
            }
        }
    }

    private void updateDispDirection(World world){
        ElevatorPartEntity part = ElevatorPartEntity.getEntityFromUUID(ref.cage, world);
        if (!(part instanceof CageEntity)) return;
        CageEntity cage = (CageEntity) part;
        cage.setCurDirection(direction);
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
            doorCloseTick = 80;
        }else{
            if (doorCloseTick >= 60){
                doorCloseTick = 80;
            }else{
                doorCloseTick = 60;
            }
        }
    }

    public boolean onLandingBlockClicked(byte index, LandingButtonBlockTE sender){
        LOGGER.info("LandingBlock");
        EnumCallingState state = EnumCallingState.NONE;
        boolean flag = false;
        if (index == curFloor && !moving){
            //エレベーターの向かってる方向と逆向きだったら普通に登録
            if (direction != EnumCallingState.convert(called.get(index))) flag = true;
            else {
                setDoorOpen();
            }
        }else flag = true;

        if (flag){
            if (index == 0) state = EnumCallingState.UP;
            else if (index == called.size() - 1){
                state = EnumCallingState.DOWN;
            }else{
                if (sender.isUp()) state = EnumCallingState.UP;
                else if (sender.isDown()) state = EnumCallingState.DOWN;
                else if (sender.isUp() && sender.isDown()) state = EnumCallingState.BOTH;
            }
            called.set(index, state);
        }else {
            if (index == 0) direction = ElevatorDirection.UP;
            else if (index == called.size() - 1) direction = ElevatorDirection.DOWN;
            else direction = sender.isUp() ? ElevatorDirection.UP : sender.isDown() ? ElevatorDirection.DOWN : ElevatorDirection.NONE;

            updateDispDirection(sender.getLevel());
        }
        LOGGER.info("State >> {}", state);

        return flag;
    }

    public boolean onEleButtonclicked(EleButtonEntity sender, boolean regVal){
        int index = sender.getFloorIndex();
        World world = sender.level;
        if (sender.level.isClientSide) return false;
        LOGGER.info("Clicked!!");
        if (moving) return false;
        if (index < 0){
            switch (index){
                case -1:
                    setDoorOpen();
                    break;
                case -2:
                    setDoorClose(world);
        }
        return true;
    }else{
        if (curFloor == index) return false;
        if (floorCount < sender.getFloorIndex()) return false;
            registered[index] = regVal;
        }
        setEleDirection();
        updateDispDirection(world);
        return true;
    }

    public void setDoorOpen(){
        LOGGER.info("Open DoorState : {}", doorState);
        resetDoorCloseTick(false);
        if (doorState == 1 || doorState == 2) {
            return;
        }

        doorState = 1;
    }

    public void setDoorClose(World world){
        if (!checkSafety(world)) return;
        if (doorState != 2) return;
        doorState = 3;

        resetDoorCloseTick(false);
    }

    public ElevatorDirection getDirection() {
        return direction;
    }

    public void updateFloor(int index, String[] displayName, int[] floorAnnounceId){
        this.displayName = displayName;
        this.floorCount = index;
    }

    public ElevatorObjectReference getRef() {
        return ref;
    }

    public NonNullList<Boolean> getIsEnabled() {
        return isEnabled;
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

    public boolean[] getRegistered() {
        return registered;
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
            fData.putBoolean("IsEnabled", isEnabled.get(i));
            //ObjHelper.checkNullAndExec(called[i2], () -> fData.putString("CallState", called[i2].name()));
            fData.putString("CallState", called.get(i).name());
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
        NonNullList<EnumCallingState> callState = NonNullList.withSize(length, EnumCallingState.NONE);
        NonNullList<Boolean> isEnabled = NonNullList.withSize(length, true);
        for (int i = 0; i < length; i++){
            CompoundNBT nbt1 = floorInfo.getCompound(i);
            yPos[i] = nbt1.getInt("Ypos");
            dispName[i] = nbt1.getString("DispName");
            registered[i] = nbt1.getBoolean("Registered");
            isEnabled.set(i, nbt1.getBoolean("IsEnabled"));
            try {
                callState.set(i, EnumCallingState.valueOf(nbt1.getString("CallState")));
            }catch (IllegalArgumentException e){
                callState.set(i, EnumCallingState.NONE);
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
        elevator.isEnabled = isEnabled;

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

    @Nullable
    public static AbstractElevator getElevatorFromUUID(UUID elevatorID, World world){
        if (!ELEVATOROFFS.containsKey(elevatorID)) return null;
        TileEntity te = world.getBlockEntity(ELEVATOROFFS.get(elevatorID));
        if (te instanceof ElevatorControllerTE) return ((ElevatorControllerTE) te).getElevator();
        return null;
    }
}
