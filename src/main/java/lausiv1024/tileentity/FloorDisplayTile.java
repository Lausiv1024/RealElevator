package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.util.ElevatorDirection;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;

import java.util.UUID;

public class FloorDisplayTile extends ElevatorPartTE implements ITickableTileEntity {
    String curFloorName = "";
    public ElevatorDirection curDirection = ElevatorDirection.NONE; //現在の方向
    ElevatorDirection renderingDirection = ElevatorDirection.NONE; //描画上の方向
    int arrowTick = 0; //矢印の動きのティック
    public int arrowFrame = 0; //矢印のアニメーションのフレーム
    int moving = 0; //0 : Stop , 1 : Slow , 2 : Fast
    int moveTimes = 0; //矢印が動き始めてから何フレーム経ったか
    boolean blinking = false;

    public String getCurFloorName() {
        return curFloorName;
    }

    public void setCurFloorName(String curFloorName) {
        this.curFloorName = curFloorName;
    }

    public FloorDisplayTile() {
        super(RETileEntities.FLOOR_DISPLAY.get(), UUID.randomUUID());
    }
    public FloorDisplayTile(UUID uuid){
        super(RETileEntities.FLOOR_DISPLAY.get(), uuid);
    }

    public void setMoving(int moving) {
        this.moving = moving;
        if (moving > 0){
            blinking = false;
        }
    }

    public int getMoving() {
        return moving;
    }

    public ElevatorDirection getRenderingDirection() {
        return renderingDirection;
    }

    public void setRenderingDirection(ElevatorDirection renderingDirection) {
        this.renderingDirection = renderingDirection;
    }

    public void clear(){
        curDirection = ElevatorDirection.NONE;
        moving = 0;
        blinking = false;
    }

    public void startBlinking(){
        blinking = true;
        arrowTick = 0;
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putString("CurrentFloor", curFloorName);
        nbt.putInt("Moving", moving);
        nbt.putInt("MoveTimes", moveTimes);
        nbt.putInt("Frame", arrowFrame);
        nbt.putInt("ArrowTick", arrowTick);
        nbt.putBoolean("Blink", blinking);
        switch (renderingDirection){
            case NONE:
                nbt.putInt("RenderDirection", 0);
                break;
            case UP:
                nbt.putInt("RenderDirection", 1);
                break;
            case DOWN:
                nbt.putInt("RenderDirection", 2);
                break;
        }
        switch (curDirection){
            case NONE:
                nbt.putInt("CurrentDirection", 0);
                break;
            case UP:
                nbt.putInt("CurrentDirection", 1);
                break;
            case DOWN:
                nbt.putInt("CurrentDirection", 2);
                break;
        }
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        curFloorName = nbt.getString("CurrentFloor");
        moving = nbt.getInt("Moving");
        moveTimes = nbt.getInt("MoveTimes");
        arrowTick = nbt.getInt("ArrowTick");
        arrowFrame = nbt.getInt("Frame");
        blinking = nbt.getBoolean("Blink");
        switch (nbt.getInt("RenderDirection")){
            case 0:
                renderingDirection = ElevatorDirection.NONE;
                break;
            case 1:
                renderingDirection = ElevatorDirection.UP;
                break;
            case 2:
                renderingDirection = ElevatorDirection.DOWN;
                break;
        }
        switch (nbt.getInt("CurrentDirection")){
            case 0:
                curDirection = ElevatorDirection.NONE;
                break;
            case 1:
                curDirection = ElevatorDirection.UP;
                break;
            case 2:
                curDirection = ElevatorDirection.DOWN;
                break;
        }
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
                renderingDirection = curDirection;
            }
            //リセット
            if (arrowTick == 26) arrowTick = 0;
            return;
        }

        //矢印が動き切ったところで消す処理
        if (curDirection == ElevatorDirection.NONE || moving == 0){
            if (arrowFrame == 0){
                arrowTick = 0;
                moveTimes = 0;
                renderingDirection = curDirection;
            }else{
                arrowTick++;
                if (arrowTick % 3 == 0){
                    updateArrowFrame();
                }
            }
            return;
        }
        //シンプルに動かす
        renderingDirection = curDirection;
        arrowTick++;
        if (arrowTick % 3 == 0){
            updateArrowFrame();
        }
    }

    //矢印の状態を変更
    private void updateArrowFrame(){
        if (arrowFrame == 7){
            arrowFrame = 0;
        }else arrowFrame++;
        moveTimes++;
    }

    @Override
    public void tick() {
        updateArrow();
    }
}
