package lausiv1024.util;

import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JitterPreventer {
    public static Map<UUID, Integer> playerTickTracker = Maps.newHashMap();

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void playerTick(final TickEvent.PlayerTickEvent event){
        if (event.phase == TickEvent.Phase.END && JitterPreventer.playerTickTracker.containsKey(event.player.getUUID()))
        {
            final Integer time = JitterPreventer.playerTickTracker.get(event.player.getUUID());
            if (time < (int) (System.currentTimeMillis() % 2000) - 100) Minecraft
                    .getInstance().options.bobView = true;
        }
        /**
         * This deals with the massive hunger reduction for standing on the
         * block entities.
         */
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.CLIENT) if (event.player.tickCount == event.player
                .getPersistentData().getInt("lastStandTick") + 1) event.player.setOnGround(true);
    }
}
