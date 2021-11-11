package lausiv1024;

import lausiv1024.tileentity.FloorDisplayTile;
import lausiv1024.tileentity.HoleLanternTile;
import lausiv1024.tileentity.LandingButtonBlockTE;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
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
public class RETileEntities {
    private static final DeferredRegister<TileEntityType<?>> TILEENTITY_TYPES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, RealElevatorCore.ID);

    public static final RegistryObject<TileEntityType<HoleLanternTile>> HOLE_LANTERN = TILEENTITY_TYPES.register("hole_lantern",() ->
            TileEntityType.Builder.of(HoleLanternTile::new, REBlocks.HOLE_LANTERN.get()).build(null));
    public static final RegistryObject<TileEntityType<FloorDisplayTile>> FLOOR_DISPLAY = TILEENTITY_TYPES.register("floor_display", () ->
            TileEntityType.Builder.of(FloorDisplayTile::new, REBlocks.FLOOR_DISPLAY.get()).build(null));
    public static final RegistryObject<TileEntityType<LandingButtonBlockTE>> LANDING_BUTTON_TE = TILEENTITY_TYPES.register("landing_button", () ->
            TileEntityType.Builder.of(LandingButtonBlockTE::new, REBlocks.ELEVATOR_BUTTON.get()).build(null));
    public static final RegistryObject<TileEntityType<LandingButtonBlockTE>> LANDING_BUTTON_SINGLE_TE = TILEENTITY_TYPES.register("landing_button_single", () ->
            TileEntityType.Builder.of(LandingButtonBlockTE::new, REBlocks.ELEVATOR_BUTTON_SINGLE.get()).build(null));

    public static void register(IEventBus eventBus){
        TILEENTITY_TYPES.register(eventBus);
    }
}
