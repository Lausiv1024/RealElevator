package lausiv1024.entity;

import lausiv1024.REEntities;
import lausiv1024.REItems;
import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.util.CageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;

public class CageEntity extends ElevatorPartEntity implements IHasCollision{
    public static final DataParameter<Integer> ROTATION_DATA = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);
    public static final DataParameter<String> CURRENT_FLOOR_DATA = EntityDataManager.defineId(CageEntity.class, DataSerializers.STRING);
    public static final DataParameter<Integer> ARROW_FRAME = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);
    public static final DataParameter<Integer> RENDER_DIRECTION = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);
    public static final DataParameter<Integer> CUR_DIRECTION = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);

    private boolean initialized = false;
    private int arrowTick;
    private int arrowSpeed;

    public CageEntity(EntityType<?> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
    }

    public CageEntity(World world){
        super(REEntities.CAGE.get(), world);
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(ROTATION_DATA, 0);
        getEntityData().define(CURRENT_FLOOR_DATA, "1");
        getEntityData().define(ARROW_FRAME, 0);
        getEntityData().define(RENDER_DIRECTION, 0);
        getEntityData().define(CUR_DIRECTION, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!initialized) initCage();
        //if (elevator == null) initialized = false;

        applyCollisionToPassenger();
        arrowTick();

        xo = getX();
        yo = getY();
        zo = getZ();
    }

    private void applyCollisionToPassenger(){
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, getBoundingBox().inflate(2),
                this::canCollideW);
        for (Entity e : entities){
            CageUtil.handleCollision(this, getEntityCollisions(), e);
        }
    }

    private void arrowTick(){
        //アニメーション処理はサーバーの言いなりになるんだよ！！クライアントで勝手に処理したらずれるにきまってんだよ！
        if (level.isClientSide) return;
        //ちな実際にクライアントに送るデータは矢印の向きとフレーム数のみをおくっている(負荷軽減?)
        if (getCurDirection() == ElevatorDirection.NONE || arrowSpeed == 0){
            if (getArrowFrame() == 0){
                arrowTick = 0;
                setRenderDirection(getCurDirection());
            }else{
                arrowTick++;
                if (arrowTick % 4 == 0){
                    updateArrowFrame();
                }
            }
            return;
        }

        setRenderDirection(getCurDirection());
        arrowTick++;

        int i = arrowSpeed == 1 ? 4 : arrowSpeed == 2 ? 8 : 0;
        if (arrowTick % i == 0){
            updateArrowFrame();
        }
    }

    private void updateArrowFrame(){
        if (getArrowFrame() == 7) {
            setArrowFrame(0);
            return;
        }
        incrementFrame();
    }

    private void initCage(){
        setCurDirection(ElevatorDirection.UP);

        initialized = true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        putRotation(nbt.getInt("Rotation"));
        putCurFloor(nbt.getString("Floor"));
        setArrowFrame(nbt.getInt("ArrowFrame"));
        setRenderDirection(ElevatorDirection.getElevatorDirectionFromIndex(nbt.getInt("RenderDirection")));
        setCurDirection(ElevatorDirection.getElevatorDirectionFromIndex(nbt.getInt("CurDirection")));
        arrowTick = nbt.getInt("ArrowTick");
        arrowSpeed = nbt.getInt("ArrowSpeed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Rotation", getRotation());
        nbt.putInt("CurDirection", getCurDirection().nbt_index);
        nbt.putString("Floor", getCurFloor());
        nbt.putInt("ArrowFrame", getArrowFrame());
        nbt.putInt("RenderDirection", getRenderDirection().nbt_index);
        nbt.putInt("ArrowTick", arrowTick);
        nbt.putInt("ArrowSpeed", arrowSpeed);
    }

    public Vector3d getPrevPos(){
        return initialized ? position() : new Vector3d(xo, yo, zo);
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return new AxisAlignedBB(getX() - 2, getY() - 1.8, getZ() - 2, getX() + 2, getY() + 5, getZ() + 2);
    }

    @Override
    public boolean hurt(DamageSource src, float p_70097_2_) {
        if (src.getEntity() instanceof PlayerEntity){
             if (((PlayerEntity) src.getEntity()).getItemInHand(Hand.MAIN_HAND).getItem() == REItems.WRENCH.get()){
                 remove();
                 return true;
             }
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected float getEyeHeight(Pose p_213316_1_, EntitySize p_213316_2_) {
        return 0.0f;
    }

    public void putCurFloor(String floor){
        if (floor.equals(getEntityData().get(CURRENT_FLOOR_DATA))) return;
        getEntityData().set(CURRENT_FLOOR_DATA, floor);
    }

    public String getCurFloor(){
        return getEntityData().get(CURRENT_FLOOR_DATA);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void putRotation(int rotation){
        if (rotation > 4 || rotation < 0){
            LOGGER.warn("Invalid rotation!! Require : (0 <= rotation < 4) Provided : {}", rotation);
            return;
        }
        entityData.set(ROTATION_DATA, rotation);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getRotation(){
        return entityData.get(ROTATION_DATA);
    }

    public void setArrowSpeed(int arrowSpeed) {
        this.arrowSpeed = arrowSpeed;
    }

    public ElevatorDirection getRenderDirection(){
        return ElevatorDirection.getElevatorDirectionFromIndex(getEntityData().get(RENDER_DIRECTION));
    }

    public void setRenderDirection(ElevatorDirection direction){
        getEntityData().set(RENDER_DIRECTION, direction.nbt_index);
    }

    public int getArrowFrame(){
        return getEntityData().get(ARROW_FRAME);
    }

    public void setArrowFrame(int frame){
        getEntityData().set(ARROW_FRAME, frame);
    }

    private void incrementFrame(){
        setArrowFrame(getArrowFrame() + 1);
    }

    public ElevatorDirection getCurDirection(){
        return ElevatorDirection.getElevatorDirectionFromIndex(getEntityData().get(CUR_DIRECTION));
    }

    public void setCurDirection(ElevatorDirection direction){
        getEntityData().set(CUR_DIRECTION, direction.nbt_index);
    }

    @Override
    public List<AxisAlignedBB> getEntityCollisions() {
        List<AxisAlignedBB> l = new ArrayList<>();
        double x = getX();
        double y = getY();
        double z = getZ();

        l.add(new AxisAlignedBB(25.0 / 16, 52.0 / 16, -27.0 / 16
                , -25.0 / 16, 60.0 / 16, 28.0 / 16));
        l.add(new AxisAlignedBB(26.0 / 16, -6.0 / 16, -29.0 / 16
                , 0, 0,0));
        l.add(new AxisAlignedBB(0, -6.0 / 16, -29.0 / 16
                , -26.0 / 16, 0, 0));
        l.add(new AxisAlignedBB(26.0 / 16, -6.0 / 16, 0,
                0, 0, 28.0 / 16));
        l.add(new AxisAlignedBB(0, -6.0 / 16, 0,
                -26.0 / 16, 0, 28.0 / 16));


        List<AxisAlignedBB> l2 = new ArrayList<>();
        l.forEach(bb -> l2.add(bb.move(x, y, z)));

        return l2;
    }
}
