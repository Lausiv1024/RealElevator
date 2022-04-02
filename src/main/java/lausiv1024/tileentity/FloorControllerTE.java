package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.networking.REPackets;
import net.minecraft.block.BlockState;
import net.minecraft.command.impl.data.DataCommand;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;

import java.util.UUID;

public class FloorControllerTE extends LandingButtonBlockTE {
    private String curFlName = "1";
    private ElevatorDirection direction = ElevatorDirection.NONE;
    private int arrowTick = 0;
    boolean moving = false;
    int moveTime = 0;
    boolean blinking = false;
    private int arrowFrame;
    private ElevatorDirection renderingDirection = ElevatorDirection.NONE;
    private int moveTimes;

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

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        curFlName = nbt.getString("FloorName");
        moveTime = nbt.getInt("MoveTime");
        moving = nbt.getBoolean("Moving");
        blinking = nbt.getBoolean("Blinking");
        arrowFrame = nbt.getInt("ArrowFrame");
        direction = ElevatorDirection.getElevatorDirectionFromIndex(nbt.getInt("CurrentDirection"));
        renderingDirection = ElevatorDirection.getElevatorDirectionFromIndex(nbt.getInt("RenderingDirection"));
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putString("FloorName", curFlName);
        nbt.putBoolean("Moving", moving);
        nbt.putInt("MoveTime", moveTime);
        nbt.putBoolean("Blinking", blinking);
        nbt.putInt("ArrowFrame", arrowFrame);
        nbt.putInt("CurrentDirection" ,direction.nbt_index);
        nbt.putInt("RenderingDirection", renderingDirection.nbt_index);
        return nbt;
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
                if (arrowTick % 4 == 0){
                    updateArrowFrame();
                }
            }
            return;
        }
        //シンプルに動かす
        renderingDirection = direction;
        arrowTick++;
        if (arrowTick % 4 == 0){
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

    public void clientUpdate(String floorName, int direction, boolean isBlink){
        curFlName = floorName;
        this.direction = ElevatorDirection.getElevatorDirectionFromIndex(direction);
        blinking = isBlink;
    }
}
