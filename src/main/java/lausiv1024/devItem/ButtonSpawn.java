package lausiv1024.devItem;

import lausiv1024.entity.EleButtonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ButtonSpawn extends DebugItem{
    public ButtonSpawn(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        EleButtonEntity eleButtonEntity = new EleButtonEntity(world);
        eleButtonEntity.setPos(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ());
        world.addFreshEntity(eleButtonEntity);
        return ActionResult.success(playerEntity.getItemInHand(hand));
    }
}
