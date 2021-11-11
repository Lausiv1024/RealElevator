package lausiv1024;

import lausiv1024.blocks.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RealElevatorCore.ID);

    public static final RegistryObject<Block> ELEVATOR_BUTTON_SINGLE = BLOCKS.register("elevator_button_single", ElevatorButtonSingleBlock::new);
    public static final RegistryObject<Block> ELEVATOR_BUTTON = BLOCKS.register("elevator_button", ElevatorButtonBlock::new);
    public static final RegistryObject<Block> DOOR_RAIL = BLOCKS.register("door_rail", DoorRail::new);
    public static final RegistryObject<Block> HOLE_LANTERN = BLOCKS.register("hole_lantern", HoleLantern::new);
    public static final RegistryObject<Block> GUIDE_RAIL = BLOCKS.register("guide_rail", GuideRailBlock::new);
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
    public static final RegistryObject<Block> GUIDE_RAILA = BLOCKS.register("guide_rail1", GuideRailA::new);
    public static final RegistryObject<Block> GUIDE_RAIL_NO_WEIGHT = BLOCKS.register("guide_rail_no_weight", GuideRailNoWeight::new);
    public static final RegistryObject<Block> FLOOR_CONTROLLER = BLOCKS.register("floor_controller", FloorController::new);
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
