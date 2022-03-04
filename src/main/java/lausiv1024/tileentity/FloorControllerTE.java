package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.RealElevator;
import lausiv1024.blocks.FloorController;
import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.elevator.EnumCallingState;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;

import java.util.UUID;

public class FloorControllerTE extends ElevatorPartTE implements ITickableTileEntity {
    private String curFlName = "1";
    private ElevatorDirection direction = ElevatorDirection.NONE;
    private int arrowTick = 0;
    boolean moving = false;
    int moveTime = 0;
    boolean blinking = false;
    private int arrowFrame;
    private ElevatorDirection renderingDirection = ElevatorDirection.NONE;
    private int moveTimes;
    private boolean up;
    private boolean down;
    private boolean called;
    private int color = 0;

    public FloorControllerTE(TileEntityType<?> tileEntityType, UUID elevatorID) {
        super(tileEntityType, elevatorID);
    }

    public FloorControllerTE(){
        super(RETileEntities.FLOOR_CONTROLLER_TE.get());
    }

    public FloorControllerTE(int color){
        super(RETileEntities.FLOOR_CONTROLLER_TE.get());
        this.color = color;
    }

    public String getCurFlName() {
        return curFlName;
    }

    public void setCurFlName(String curFlName) {
        this.curFlName = curFlName;
    }

    public ElevatorDirection getDirection() {
        return direction;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public boolean isCalled() {
        return called;
    }

    public int getColor() {
        return color;
    }

    public void upA(){
        up = true;
        called = true;
    }

    public void dwA(){
        down = true;
        called = true;
    }

    public void setCalled(boolean called) {
        if (getBlockState().getValue(FloorController.IS_SINGLE))
            this.called = called;
    }

    public void rm(boolean downM, boolean upM){
        down = !downM;
        up = !upM;
        if (getBlockState().getValue(FloorController.IS_SINGLE)) return;
        if (!down && !up) called = false;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        curFlName = nbt.getString("FloorName");
        moveTime = nbt.getInt("MoveTime");
        moving = nbt.getBoolean("Moving");
        blinking = nbt.getBoolean("Blinking");
        arrowFrame = nbt.getInt("ArrowFrame");
        direction = ElevatorDirection.getElevatorDirectionFromIndex(nbt.getInt("CurrentDirection"));
//        try {
//            registeredDirection = Enum.valueOf(EnumCallingState.class, nbt.getString("RegisteredDirection"));
//        }catch (IllegalArgumentException e){
//            registeredDirection = EnumCallingState.NONE;
//            RealElevator.LOGGER.info("Registered Direction is Empty or invalid . None was inserted");
//        }
        renderingDirection = ElevatorDirection.getElevatorDirectionFromIndex(nbt.getInt("RenderingDirection"));
        up = nbt.getBoolean("Up");
        down = nbt.getBoolean("Down");
        called = nbt.getBoolean("Called");
        color = nbt.getInt("Color");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putString("FloorName", curFlName);
        nbt.putBoolean("Moving", moving);
        nbt.putInt("MoveTime", moveTime);
        nbt.putBoolean("Blinking", blinking);
        nbt.putInt("ArrowFrame", arrowFrame);
        nbt.putInt("CurrentDirection" ,direction.nbt_index);
        nbt.putInt("RenderingDirection", renderingDirection.nbt_index);
        nbt.putBoolean("Up", up);
        nbt.putBoolean("Down", down);
        nbt.putBoolean("Called", called);
        nbt.putInt("Color", color);
        return super.save(nbt);
    }

    @Override
    public void tick() {
        updateArrow();
    }

    private void updateArrow(){
        //F*jitecのエレベーターの矢印の挙動を再現
        if (blinking){
            //到着などで点滅しているとき
            arrowTick++;
            arrowFrame = 0;
            //21tickより上で消灯
            if (arrowTick >= 21){
                renderingDirection = ElevatorDirection.NONE;
            }else{
                //現在の状態で点灯
                renderingDirection = direction;
            }
            //リセット
            if (arrowTick == 26) arrowTick = 0;
            return;
        }

        //矢印が動き切ったところで消す処理
        if (direction == ElevatorDirection.NONE || !moving){
            if (arrowFrame == 0){
                arrowTick = 0;
                moveTimes = 0;
                renderingDirection = direction;
            }else{
                arrowTick++;
                if (arrowTick % 3 == 0){
                    updateArrowFrame();
                }
            }
            return;
        }
        //シンプルに動かす
        renderingDirection = direction;
        arrowTick++;
        if (arrowTick % 3 == 0){
            updateArrowFrame();
        }
    }

    public ElevatorDirection getRenderingDirection() {
        return renderingDirection;
    }

    public int getArrowFrame() {
        return arrowFrame;
    }

    public void setDirection(ElevatorDirection direction) {
        this.direction = direction;
    }

    //矢印の状態を変更
    private void updateArrowFrame(){
        if (arrowFrame == 7){
            arrowFrame = 0;
        }else arrowFrame++;
        moveTimes++;
    }
}
