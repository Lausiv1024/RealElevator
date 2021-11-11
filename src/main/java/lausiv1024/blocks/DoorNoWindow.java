package lausiv1024.blocks;

import lausiv1024.util.EleVeneerType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;

public class DoorNoWindow extends Block {
    public static final EnumProperty<EleVeneerType> TYPE = EnumProperty.create("type", EleVeneerType.class);
    public DoorNoWindow(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(TYPE, EleVeneerType.OAK));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }
}
