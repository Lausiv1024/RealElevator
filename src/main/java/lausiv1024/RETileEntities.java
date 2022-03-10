package lausiv1024;

import lausiv1024.tileentity.*;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RETileEntities {
    private static final DeferredRegister<TileEntityType<?>> TILEENTITY_TYPES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, RealElevator.ID);

    public static final RegistryObject<TileEntityType<HoleLanternTile>> HOLE_LANTERN = TILEENTITY_TYPES.register("hole_lantern",() ->
            TileEntityType.Builder.of(HoleLanternTile::new, REBlocks.HOLE_LANTERN.get()).build(null));
    public static final RegistryObject<TileEntityType<FloorDisplayTile>> FLOOR_DISPLAY = TILEENTITY_TYPES.register("floor_display", () ->
            TileEntityType.Builder.of(FloorDisplayTile::new, REBlocks.FLOOR_DISPLAY.get()).build(null));
    public static final RegistryObject<TileEntityType<LandingButtonBlockTE>> LANDING_BUTTON_TE = TILEENTITY_TYPES.register("landing_button", () ->
            TileEntityType.Builder.of(LandingButtonBlockTE::new, REBlocks.ELEVATOR_BUTTON.get()).build(null));
    public static final RegistryObject<TileEntityType<BoundingBlockTE>> BOUNDING_BLOCK_TE = TILEENTITY_TYPES.register("bounding_block",
            () -> TileEntityType.Builder.of(BoundingBlockTE::new, REBlocks.BOUNDING_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<FloorControllerTE>> FLOOR_CONTROLLER_TE = TILEENTITY_TYPES.register("floor_controller",() ->
            TileEntityType.Builder.of(FloorControllerTE::new, REBlocks.FLOOR_CONTROLLER.get()).build(null));
    public static final RegistryObject<TileEntityType<ElevatorControllerTE>> ELEVATOR_CONTROLLER = TILEENTITY_TYPES.register("elevator_controller", () ->
            TileEntityType.Builder.of(ElevatorControllerTE::new, REBlocks.ELEVATOR_CONTROLLER.get()).build(null));

    public static void register(IEventBus eventBus){
        TILEENTITY_TYPES.register(eventBus);
    }
}
