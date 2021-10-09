package lausiv1024.devItem;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class MobAttacked extends Item {
    public MobAttacked(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        AxisAlignedBB bb = new AxisAlignedBB(playerEntity.position().x - 70, playerEntity.position().y - 70, playerEntity.position().z - 70,
                playerEntity.position().x + 70, playerEntity.position().y + 70, playerEntity.position().z + 70);
        List<LivingEntity> livingEntities = world.getEntitiesOfClass(LivingEntity.class, bb);
        for (LivingEntity entity : livingEntities) {
            entity.hurt(DamageSource.playerAttack(playerEntity), 0f);
        }
        return ActionResult.success(playerEntity.getMainHandItem());
    }
}
