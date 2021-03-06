package lausiv1024.devItem;

import lausiv1024.RealElevator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REDevItems{
    private static final Item.Properties DEFAULT = new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(1);
    public static final DeferredRegister<Item> DEV_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RealElevator.ID);

    public static final RegistryObject<Item> DEBUG_BUTTON_SPAWN = DEV_ITEMS.register("get_cage_pos",() -> new GetCagePos(DEFAULT));
    public static final RegistryObject<Item> DEBUG_BOUNDINGBOX_GETTER = DEV_ITEMS.register("boundingbox_getter",() ->
            new BoundingBoxGetter(DEFAULT));
    public static final RegistryObject<Item> ENTITY_POS_GETTER = DEV_ITEMS.register("pos_getter",() ->
            new EntityPosGetter(DEFAULT));
    public static final RegistryObject<Item> SUMMON_DOOR = DEV_ITEMS.register("kill_cage",() ->
            new KillCage(DEFAULT));
    public static final RegistryObject<Item> MOB_ATTACKED = DEV_ITEMS.register("mob_attacked", () ->
            new MobAttacked(DEFAULT));

    public static void register(IEventBus eventBus){
        DEV_ITEMS.register(eventBus);
    }
}
