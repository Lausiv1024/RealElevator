package lausiv1024;

import lausiv1024.tileentity.HoleLanternTile;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.TypeReferences;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RETileEntities {
    public static final List<TileEntityType<? extends TileEntity>> TILE_ENTITY_TYPES = new ArrayList<>();

    public static final TileEntityType<?> HOLE_LANTERN = register("hole_lantern",
            TileEntityType.Builder.of(HoleLanternTile::new, REBlocks.HOLE_LANTERN).build(Util.fetchChoiceType(TypeReferences.BLOCK_ENTITY, "hole_lantern")));

    private static TileEntityType<? extends TileEntity> register(String registryName, TileEntityType<?> tileEntityType){
        TileEntityType named = tileEntityType.setRegistryName(registryName);
        TILE_ENTITY_TYPES.add(named);
        return named;
    }

    @SubscribeEvent
    public static void registerTileEntityType(RegistryEvent.Register<TileEntityType<?>> event){
        for (TileEntityType<?> tileEntityType : TILE_ENTITY_TYPES){
            event.getRegistry().register(tileEntityType);
        }
    }
}
