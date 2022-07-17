package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.elevator.AbstractElevator;
import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.networking.LandingDispRenderUpdate;
import lausiv1024.networking.LandingDispUpdateMsg;
import lausiv1024.networking.REPackets;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
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
        decideBlinking();
        updateArrow();
    }

    private void decideBlinking(){
        AbstractElevator elevator = AbstractElevator.getElevatorFromUUID(elevatorID, level);
        if (elevator == null) return;
        blinking = elevator.getCurFloor() == floorIndex && elevator.getDoorState() > 0 &&
                elevator.getDirection() != ElevatorDirection.NONE;
    }

    private void updateArrow(){
        //F*jitecのエレベーターの矢印の挙動を再現
        if (blinking){
            //到着などで点滅しているとき
            arrowTick++;
            arrowFrame = 0;
            //21tickより上で消灯
            if (arrowTick >= 18){
                if (renderingDirection != ElevatorDirection.NONE) {
                    renderingDirection = ElevatorDirection.NONE;
                    sendDispUpdatePacket();
                }
            }else{
                //現在の状態で点灯
                if (renderingDirection != direction) {
                    renderingDirection = direction;
                    sendDispUpdatePacket();
                }
            }
            //リセット
            if (arrowTick == 24) arrowTick = 0;
            return;
        }

        //矢印が動き切ったところで消す処理
        if (direction == ElevatorDirection.NONE || !moving){
            if (arrowFrame == 0){
                arrowTick = 0;
                moveTimes = 0;
                if (renderingDirection != direction) {
                    renderingDirection = direction;
                    sendDispUpdatePacket();
                }
            }else{
                arrowTick++;
                if (arrowTick % 4 == 0){
                    updateArrowFrame();
                }
            }
            return;
        }
        //シンプルに動かす
        if (renderingDirection != direction) {
            renderingDirection = direction;
            sendDispUpdatePacket();
        }
        arrowTick++;
        if (arrowTick % 4 == 0){
            updateArrowFrame();
        }
    }

    public void sendDispUpdatePacket(){//毎tick呼ぶと多分死ぬ
        if (level.isClientSide) return;
//        RealElevator.LOGGER.info("Packet of DispUpdate");
        REPackets.CHANNEL.send(target(), new LandingDispUpdateMsg(this, curFlName, (byte) renderingDirection.nbt_index, (byte) arrowFrame, blinking));
        REPackets.CHANNEL.send(target(), new LandingDispRenderUpdate(this, renderingDirection));
    }

    public ElevatorDirection getRenderingDirection() {
        return renderingDirection;
    }

    public int getArrowFrame() {
        return arrowFrame;
    }

    public void setDirection(ElevatorDirection direction, boolean sync) {
        if (this.direction == direction) return;
        this.direction = direction;
        if (sync)
            sendDispUpdatePacket();
    }

    public String getCurFlName() {
        return curFlName;
    }

    public void setCurFlName(String curFlName) {
        if (curFlName.equals(this.curFlName)) return;
        this.curFlName = curFlName;
        sendDispUpdatePacket();
    }

    public ElevatorDirection getDirection() {
        return direction;
    }

    public boolean isBlinking() {
        return blinking;
    }

    public void setBlinking(boolean blinking, boolean sync) {
        if (this.blinking == blinking) return;
        this.blinking = blinking;
        if (sync)
            sendDispUpdatePacket();
    }

    public void setMoving(boolean moving, boolean sync) {
        if (this.moving == moving) return;
        this.moving = moving;
        if (sync)
            sendDispUpdatePacket();
    }

    //矢印の状態を変更
    private void updateArrowFrame(){
        if (arrowFrame == 7){
            arrowFrame = 0;
        }else arrowFrame++;
        moveTimes++;
        sendDispUpdatePacket();
    }

    public void clientUpdate(String floorName, byte direction,byte arrowFrame, boolean isBlink){
//        RealElevator.LOGGER.info("ClientUpdate  Detail >> (floorname:{} , direction : {}, arrowFrame : {}, isBlinking : {}", floorName, direction, arrowFrame, isBlink);
        curFlName = floorName;
        this.direction = ElevatorDirection.getElevatorDirectionFromIndex(direction);
        blinking = isBlink;
        this.arrowFrame = arrowFrame;
    }

    public void renderUpdate(ElevatorDirection renderDirection){
        if (renderDirection == this.renderingDirection) return;
        this.renderingDirection = renderDirection;
    }
}
