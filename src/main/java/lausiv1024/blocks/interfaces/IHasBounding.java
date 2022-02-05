package lausiv1024.blocks.interfaces;

import net.minecraft.util.math.BlockPos;

public interface IHasBounding {
    //It is a marker of Bounding Block.  This Interface is read by ElevatorPartBlock#onPlace and ElevatorPartBlock#onRemove
    BlockPos[] getBoundingPosList();
}
