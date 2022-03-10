package lausiv1024;

import lausiv1024.blocks.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RealElevator.ID);

    public static final RegistryObject<Block> ELEVATOR_BUTTON = BLOCKS.register("elevator_button", ElevatorButtonBlock::new);
    public static final RegistryObject<Block> DOOR_THRESHOLD = BLOCKS.register("door_threshold", DoorRail::new);
    public static final RegistryObject<Block> HOLE_LANTERN = BLOCKS.register("hole_lantern", HoleLantern::new);
    public static final RegistryObject<Block> GUIDE_RAIL_CAGE = BLOCKS.register("guide_rail_cage", GuideRailBlockCage::new);
    public static final RegistryObject<Block> GUIDE_RAIL_COUNTER_WEIGHT = BLOCKS.register("guide_rail_counter_weight", GuideRailBlockCounterWeight::new);
    public static final RegistryObject<Block> ELEVATOR_MARKER = BLOCKS.register("elevator_marker",() ->
            new ElevatorMarker(AbstractBlock.Properties.of(Material.METAL).
                    strength(2, 10).harvestTool(ToolType.PICKAXE).harvestLevel(2)));
    public static final RegistryObject<Block> RANGE_DISPLAY = BLOCKS.register("range_display",() ->
            new RangeDisplay(AbstractBlock.Properties.of(Material.METAL)));
    public static final RegistryObject<Block> ELEVATOR_CONSTRUCTOR = BLOCKS.register("elevator_constructor",() ->
            new ElevatorConstructor(AbstractBlock.Properties.of(Material.METAL)
            .strength(0.6f, 10).harvestTool(ToolType.PICKAXE).harvestLevel(2)));
    public static final RegistryObject<Block> FLOOR_DISPLAY = BLOCKS.register("floor_display", FloorDisplay::new);
    public static final RegistryObject<Block> DOOR_RAIL_DOUBLE = BLOCKS.register("door_rail_double", DoorRailDouble::new);
    public static final RegistryObject<Block> ELEVATOR_CONTROLLER = BLOCKS.register("controller_module", ElevatorControllerBlock::new);
    public static final RegistryObject<Block> FLOOR_CONTROLLER = BLOCKS.register("floor_controller", FloorController::new);
    public static final RegistryObject<Block> JAMB = BLOCKS.register("jamb", Jamb::new);
    public static final RegistryObject<Block> JAMB_ROOF = BLOCKS.register("jamb_roof", JambRoof::new);
    public static final RegistryObject<Block> MOTOR = BLOCKS.register("motor", Motor::new);
    public static final RegistryObject<Block> BOUNDING_BLOCK = BLOCKS.register("bounding_block", () -> new BoundingBlock(AbstractBlock.Properties.of(Material.METAL)));
//
//    public static final RegistryObject<Block> GOMI = BLOCKS.register("elevatordoor_no_window31", () -> new Block(AbstractBlock.Properties.of(Material.WOOL)));
//    public static final RegistryObject<Block> GOMI2 = BLOCKS.register("elevatordoor_no_window32", () -> new Block(AbstractBlock.Properties.of(Material.WOOL)));
//    public static final RegistryObject<Block> GOMI3 = BLOCKS.register("elevatordoor_no_window41", () -> new Block(AbstractBlock.Properties.of(Material.WOOL)));
//    public static final RegistryObject<Block> GOMI4 = BLOCKS.register("elevatordoor_no_window42", () -> new Block(AbstractBlock.Properties.of(Material.WOOL)));
    public static final RegistryObject<Block> EDNW = BLOCKS.register("door_nowindow13", () -> new DoorNoWindow(AbstractBlock.Properties.of(Material.WOOL)));

    public static void register(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }
}
