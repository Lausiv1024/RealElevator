package lausiv1024;

import lausiv1024.blocks.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;
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

    public static final Block ELEVATOR_BUTTON_SINGLE = register("elevator_button_single", new ElevatorButtonSingleBlock());
    public static final Block ELEVATOR_BUTTON = register("elevator_button", new ElevatorButtonBlock());
    public static final Block DOOR_RAIL = register("door_rail", new DoorRail());
    public static final Block HOLE_LANTERN = register("hole_lantern", new HoleLantern());
    public static final Block GUIDE_RAIL = register("guide_rail", new GuideRailBlock());
    public static final Block ELEVATOR_MARKER = register("elevator_marker",
            new ElevatorMarker(AbstractBlock.Properties.of(Material.METAL).
                    strength(2, 10).harvestTool(ToolType.PICKAXE).harvestLevel(2)));
    public static final Block RANGE_DISPLAY = register("range_display",
            new RangeDisplay(AbstractBlock.Properties.of(Material.METAL)));
    public static final Block ELEVATOR_CONSTRUCTOR = register("elevator_constructor",
            new ElevatorConstructor(AbstractBlock.Properties.of(Material.METAL)
            .strength(0.6f, 10).harvestTool(ToolType.PICKAXE).harvestLevel(2)));
    public static final Block FLOOR_DISPLAY = register("floor_display",
            new FloorDisplay());
    public static final Block DOOR_RAIL_DOUBLE = register("door_rail_double",
            new DoorRailDouble());
    public static final Block ELEVATOR_CONTROLLER = register("controller_module",
            new ElevatorControllerBlock());
    public static final Block GUIDE_RAILA = register("guide_rail1",
            new GuideRailA());
    public static final Block GUIDE_RAIL_NO_WEIGHT = register("guide_rail_no_weight",
            new GuideRailNoWeight());

    @SubscribeEvent
    public static void registerBlocks(final RegistryEvent.Register<Block> event){
        for (Block block : BLOCK_LIST){
            event.getRegistry().register(block);
        }
    }
}
