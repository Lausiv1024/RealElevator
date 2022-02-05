package lausiv1024.devItem;

import lausiv1024.RealElevator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class EntityPosGetter extends DebugItem{
    public EntityPosGetter(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity entity, Hand hand) {
        double x = entity.position().x;
        double y = entity.position().y;
        double z = entity.position().z;
        AxisAlignedBB bb = new AxisAlignedBB(x - 4, y - 4, z - 4, x + 4, y + 4, z + 4);
        entity.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 1);
        List<Entity> entities = world.getEntities((Entity) null, bb, (entity1) -> !(entity1 instanceof PlayerEntity));
        for (Entity entity1 : entities){
            RealElevator.LOGGER.info(entity1.getName().getString() + " : " + entity1.getBoundingBox().toString());
        }
        return ActionResult.success(entity.getItemInHand(hand));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity player, Entity entity) {
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 1);

        return true;
    }
}
