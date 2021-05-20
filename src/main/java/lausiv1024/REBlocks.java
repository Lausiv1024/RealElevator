package lausiv1024;

import lausiv1024.blocks.DoorRail;
import lausiv1024.blocks.ElevatorButton;
import lausiv1024.blocks.ElevatorButtonSingle;
import lausiv1024.blocks.HoleLantern;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REBlocks {
    public static final List<Block> BLOCK_LIST = new ArrayList<>();

    private static Block register(String registryName, Block block){
        Block blockNamed = block.setRegistryName(RealElevatorCore.ID, registryName);
        BLOCK_LIST.add(blockNamed);
        return blockNamed;
    }

    public static final Block ELEVATOR_BUTTON_SINGLE = register("elevator_button_single", new ElevatorButtonSingle());
    public static final Block ELEVATOR_BUTTON = register("elevator_button", new ElevatorButton());
    public static final Block DOOR_RAIL = register("door_rail", new DoorRail());
    public static final Block HOLE_LANTERN = register("hole_lantern", new HoleLantern());


    @SubscribeEvent
    public static void registerBlocks(final RegistryEvent.Register<Block> event){
        for (Block block : BLOCK_LIST){
            event.getRegistry().register(block);
        }
    }
}
