package lausiv1024.entity;

import lausiv1024.REEntities;
import lausiv1024.util.CageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;

public class CageEntity extends ElevatorPartEntity implements IHasCollision{
    public static final DataParameter<Integer> ROTATION_DATA = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);
    public static final DataParameter<Boolean> ON = EntityDataManager.defineId(CageEntity.class, DataSerializers.BOOLEAN);
    public static final DataParameter<String> CURRENT_FLOOR_DATA = EntityDataManager.defineId(CageEntity.class, DataSerializers.STRING);
    public static final DataParameter<Integer> ARROW_DATA = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);

    private boolean initialized = false;

    public CageEntity(EntityType<?> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
    }

    public CageEntity(World world){
        super(REEntities.CAGE.get(), world);
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(ROTATION_DATA, 0);
        getEntityData().define(ON, false);
        getEntityData().define(CURRENT_FLOOR_DATA, "1");
        getEntityData().define(ARROW_DATA, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!initialized) initCage();
        if (elevator == null) initialized = false;

        applyCollisionToPassenger();

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

    private void initCage(){
        initialized = true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        putRotation(nbt.getInt("Rotation"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Rotation", getRotation());
    }

    public Vector3d getPrevPos(){
        return initialized ? position() : new Vector3d(xo, yo, zo);
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return new AxisAlignedBB(getX() - 2, getY() - 1.8, getZ() - 2, getX() + 2, getY() + 5, getZ() + 2);
    }

    @Override
    protected float getEyeHeight(Pose p_213316_1_, EntitySize p_213316_2_) {
        return 0.0f;
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

    @Override
    public List<AxisAlignedBB> getEntityCollisions() {
        List<AxisAlignedBB> l = new ArrayList<>();
        double x = getX();
        double y = getY();
        double z = getZ();
//        l.add(new AxisAlignedBB(x + 27.0 / 16.0, y - 8.0 / 16.0, z - 29.0 / 16
//                , x - 27.0 / 16.0, y, z + 27.0 / 16));//Floor
//        l.add(new AxisAlignedBB(x + 25.0 / 16.0, y + 52.0 / 16.0, z - 27.0 / 16.0,
//                x - 25.0 / 16.0, y + 60.0 / 16.0, z + 28.0 / 16.0));//Roof
//        List<AxisAlignedBB> wallLocals = new ArrayList<>();
//        wallLocals.add(REMathHelper.rotateLocalAABB(new AxisAlignedBB(24.0 / 16, -8.0 / 16, -27.0 / 16.0,
//                12.0 / 16, 46.0 / 16, -25.0 / 16), getRotation()));
//        wallLocals.add(REMathHelper.rotateLocalAABB(new AxisAlignedBB(-12.0 / 16, -8.0 / 16, -27.0 / 16,
//                -24.0 / 16, 46 / 16.0, -25 / 16.0), getRotation()));
//        wallLocals.add(REMathHelper.rotateLocalAABB(new AxisAlignedBB(24.0 / 16,  -8.0 / 16, -25.0 / 16,
//                22.0 / 16, 46.0 / 16, 25.0 / 16), getRotation()));
//        wallLocals.add(REMathHelper.rotateLocalAABB(new AxisAlignedBB(22.0 / 16,  -8.0 / 16, 26.0 / 16,
//                -22.0 / 16, 46.0 / 16, 28.0 / 16), getRotation()));
//        wallLocals.add(REMathHelper.rotateLocalAABB(new AxisAlignedBB(-22.0 / 16,  -8.0 / 16, -25.0 / 16,
//                -26.0 / 16, 46.0 / 16, 25.0 / 16), getRotation()));
//        wallLocals.forEach(item -> l.add(new AxisAlignedBB(item.minX + x, item.minY + y, item.minZ + z,
//                item.maxX + x, item.maxY + y, item.maxZ + z)));

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
