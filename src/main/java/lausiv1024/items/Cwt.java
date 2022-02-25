package lausiv1024.items;

import lausiv1024.entity.CwtEntity;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
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

    @Override
    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity player, Entity entity) {
        if (entity instanceof LivingEntity){
            LivingEntity entity1 = (LivingEntity) entity;
            entity1.hurt(DamageSource.ANVIL, Float.MAX_VALUE);
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int p_77663_4_, boolean p_77663_5_) {
        if (entity instanceof PlayerEntity){
            PlayerEntity player = (PlayerEntity) entity;
            EffectInstance instance = new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 20, 4, true, true);
            //player.addEffect(instance);
        }
    }
}
