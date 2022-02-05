package lausiv1024.entity.doors;

import lausiv1024.REEntities;
import lausiv1024.RealElevator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class ElevatorDoorNoWindow extends AbstractElevatorDoorEntity {

    public BlockState blockState = Blocks.AIR.defaultBlockState();

    public ElevatorDoorNoWindow(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    public ElevatorDoorNoWindow(World world){
        super(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), world);
        tall = 3;
        setDirection(Direction.NORTH);
        applyDirection();
    }

    public ElevatorDoorNoWindow (World world, Direction direction){
        super(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), world);
        setDirection(direction);
        tall = 3;
        applyDirection();
        getBoundingBox();
    }

    public ElevatorDoorNoWindow(World world, Direction direction, int tall){
        super(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), world);
        RealElevator.LOGGER.info("Cons5");
        this.tall = tall;
        setDirection(direction);
        applyDirection();
        getBoundingBox();
    }

    @Override
    protected void applyDirection() {
        super.applyDirection();
    }

    @Override
    public void tick() {
        super.tick();
    }


    @Override
    public ActionResultType interact(PlayerEntity playerEntity, Hand hand) {
        World level = playerEntity.level;
        if (playerEntity.getItemInHand(hand).getItem() == Items.STICK){
            Vector3d pos = position();
            ElevatorDoorNoWindow elevatorDoorNoWindow = new ElevatorDoorNoWindow(level, Direction.SOUTH);
            elevatorDoorNoWindow.setPos(pos.x, pos.y, pos.z);
            level.addFreshEntity(elevatorDoorNoWindow);
            this.remove();
            return ActionResultType.SUCCESS;
        }else if (playerEntity.getItemInHand(hand).getItem() == Items.REDSTONE){
            Vector3d pos = position();
            ElevatorDoorNoWindow elevatorDoorNoWindow = new ElevatorDoorNoWindow(level, Direction.EAST);
            elevatorDoorNoWindow.setPos(pos.x, pos.y, pos.z);
            level.addFreshEntity(elevatorDoorNoWindow);
            this.remove();
            return ActionResultType.SUCCESS;
        }else if (playerEntity.getItemInHand(hand).getItem() instanceof BlockItem){
            BlockItem blockItem = (BlockItem) playerEntity.getItemInHand(hand).getItem();
            blockState = blockItem.getBlock().defaultBlockState();
        }else{
            test(playerEntity, hand, level);
        }
        return ActionResultType.PASS;
    }

    private void test(PlayerEntity playerEntity, Hand hand, World level){

    }

    @Override
    public boolean hurt(DamageSource source, float p_70097_2_) {
        if (source.getEntity() instanceof  PlayerEntity){
            PlayerEntity player = (PlayerEntity) source.getEntity();
            if (player.getItemInHand(Hand.MAIN_HAND).getItem() == Items.STICK){
                if (this.isAlive() && !level.isClientSide){
                    remove();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return super.getBoundingBox();
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        tall = nbt.getInt("Tall");
        surfaceType = nbt.getInt("SurfaceType");
        int direI = nbt.getInt("Direction");

        blockState = NBTUtil.readBlockState(nbt.getCompound("Block"));

        switch (direI){
            case 0:
                setDirection(Direction.NORTH);
                break;
            case 1:
                setDirection(Direction.SOUTH);
                break;
            case 2:
                setDirection(Direction.EAST);
                break;
            case 3:
                setDirection(Direction.WEST);
                break;
        }
        applyDirection();
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Tall", tall);
        nbt.putInt("SurfaceType", surfaceType);

        nbt.put("Block", NBTUtil.writeBlockState(blockState));

        switch (this.getDirection()){
            case NORTH:
                nbt.putInt("Direction", 0);
                break;
            case SOUTH:
                nbt.putInt("Direction", 1);
                break;
            case EAST:
                nbt.putInt("Direction", 2);
                break;
            case WEST:
                nbt.putInt("Direction", 3);
                break;
        }
    }
}
