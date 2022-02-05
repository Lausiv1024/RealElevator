package lausiv1024.items;

import lausiv1024.entity.CwtEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class Cwt extends ElevatorPartItem{

    @Override
    protected String getInfo() {
        return null;
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity playerEntity, Hand hand) {
        CwtEntity entity = new CwtEntity(level);
        entity.setPos(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ());
        level.addFreshEntity(entity);
        return ActionResult.consume(playerEntity.getItemInHand(hand));
    }
}
