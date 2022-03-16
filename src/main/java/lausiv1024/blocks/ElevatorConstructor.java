package lausiv1024.blocks;

import lausiv1024.REBlocks;
import lausiv1024.RealElevator;
import lausiv1024.entity.CageEntity;
import lausiv1024.entity.doors.ElevatorDoorNoWindow;
import net.minecraft.block.*;
import net.minecraft.command.impl.FillCommand;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Vector;

public class ElevatorConstructor extends ContainerBlock {
    private static final DirectionProperty FACING = HorizontalBlock.FACING;
    public ElevatorConstructor(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult result) {
        assemble(state.getValue(FACING).getOpposite(), pos, world);
        return ActionResultType.SUCCESS;
    }

    private void assemble(Direction direction, BlockPos pos, World world){
        LOGGER.info(pos);
        int yPos = pos.getY();
        int[] floorHeights = {yPos, yPos + 5, yPos + 10};
        String[] floorName = {"1", "2", "3"};
        int floorEndIndex = floorHeights.length - 1;
        BlockPos off = pos.offset(new BlockPos(0, 0, -2));
        BlockPos flConLocal = new BlockPos(4, 0, 2);
        Vector3d localCagePos = new Vector3d(3.0, 0, -3.0);
        CageEntity cage = new CageEntity(world);
        Vector3d cagePosG = localCagePos.add(pos.getX(), pos.getY(), pos.getZ());
        cage.setPos(cagePosG.x, cagePosG.y, cagePosG.z);
        world.addFreshEntity(cage);
        createCageDoor(direction, off, world);

        digPit(direction, pos, world);
        for (int i = 0; i < floorHeights.length; i++){
            boolean isSingleBut = i == 0 || i == floorHeights.length - 1;
            createThreshould(direction, off, world, floorHeights[i]);
            createDoorHeader(direction, off, world, floorHeights[i]);
            createJamb(direction, off, world, floorHeights[i]);
            createController(direction, off, world, floorHeights[i], isSingleBut);
            createFloorDoor(direction, off, world, floorHeights[i]);
        }
    }

    private void createThreshould(Direction direction, BlockPos off, World world, int yPos){
        LOGGER.info("Offset -> {}, yPos -> {}", off, yPos);
        for (int i = 1; i < 5; i++){
            BlockPos willPlace = new BlockPos(off.getX() + i, yPos - 1, off.getZ());
            LOGGER.info("WillPlace ->{}", willPlace);
            world.setBlockAndUpdate(willPlace, REBlocks.DOOR_THRESHOLD.get().defaultBlockState().setValue(FACING, direction.getOpposite()));
        }
    }

    private void createDoorHeader(Direction direction, BlockPos off, World world, int yPos){
        BlockPos of2 = new BlockPos(off.getX() + 2, yPos + 3, off.getZ());
        BlockPos of3 = new BlockPos(off.getX() + 3, yPos + 3, off.getZ());
        world.setBlockAndUpdate(of2, REBlocks.DOOR_HEADER.get().defaultBlockState().setValue(FACING, direction.getOpposite()).setValue(DoorHeader.MIRROR, true));
        world.setBlockAndUpdate(of3, REBlocks.DOOR_HEADER.get().defaultBlockState().setValue(FACING, direction.getOpposite()));
    }

    private void createJamb(Direction direction, BlockPos offset, World world, int yPos){
        for (int i = 0; i < 3; i++){
            BlockPos of1 = new BlockPos(offset.getX() + 2, yPos + i, offset.getZ() + 1);
            BlockPos of2 = new BlockPos(offset.getX() + 3, yPos + i, offset.getZ() + 1);
            BlockState state = i == 2 ? REBlocks.JAMB.get().defaultBlockState().setValue(Jamb.HAS_ROOF, true).setValue(FACING, direction) :
                    REBlocks.JAMB.get().defaultBlockState().setValue(FACING, direction);
            world.setBlockAndUpdate(of1, state);
            world.setBlockAndUpdate(of2, state.setValue(Jamb.IS_MIRROR, true));
        }
    }

    private void createCageDoor(Direction direction, BlockPos pos, World world){
        Vector3d of1 = new Vector3d(pos.getX() + 2.625, pos.getY() , pos.getZ() + 0.75);
        Vector3d of2 = of1.add(0.75, 0, 0);
        ElevatorDoorNoWindow door1 = new ElevatorDoorNoWindow(world, direction.getOpposite());
        ElevatorDoorNoWindow door2 = new ElevatorDoorNoWindow(world, direction.getOpposite());
        door1.setPos(of1);
        door2.setPos(of2);
        world.addFreshEntity(door1);
        world.addFreshEntity(door2);
    }

    private void createController(Direction direction, BlockPos pos, World world, int yPos, boolean isSingleBut){
        BlockPos of = new BlockPos(pos.getX() + 4, yPos + 1, pos.getZ() + 2);
        world.setBlockAndUpdate(of, REBlocks.FLOOR_CONTROLLER.get().defaultBlockState().setValue(FACING, direction)
                .setValue(FloorController.IS_SINGLE, isSingleBut));
    }

    private void createFloorDoor(Direction direction, BlockPos pos, World world, int yPos){
        Vector3d of1 = new Vector3d(pos.getX() + 2.625, yPos, pos.getZ() + 0.94);
        Vector3d of2 = new Vector3d(pos.getX() + 3.375, yPos, pos.getZ() + 0.94);
        ElevatorDoorNoWindow door = new ElevatorDoorNoWindow(world, direction);
        ElevatorDoorNoWindow door2 = new ElevatorDoorNoWindow(world, direction);
        door.setPos(of1);
        door2.setPos(of2);
        world.addFreshEntity(door);
        world.addFreshEntity(door2);
    }

    private void digPit(Direction direction, BlockPos pos, World world){
        BlockPos st = new BlockPos(pos.getX(), pos.getY() - 2, pos.getZ() - 2);
        BlockPos end = st.offset(5, 1, -4);
        blockFill(st, end, world, Blocks.AIR.defaultBlockState());
    }

    private void blockFill(BlockPos start, BlockPos end, World world, BlockState state){
        MutableBoundingBox box = new MutableBoundingBox(start, end);
        Iterator<BlockPos> posIterator = BlockPos.betweenClosed(box.x0, box.y0, box.z0, box.x1, box.y1, box.z1).iterator();
        BlockPos pos;
        while (posIterator.hasNext()){
            pos = posIterator.next();
            if (!world.isClientSide)
                world.setBlockAndUpdate(pos, state);
        }
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockRenderType getRenderShape(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(IBlockReader p_196283_1_) {
        return null;
    }
}
