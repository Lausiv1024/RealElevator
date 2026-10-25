package net.lausiv1024.realelevator.client;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.entity.ElevatorClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * クライアントのtickごとに、かごの時計（{@link ElevatorClock}）を進める。
 */
@EventBusSubscriber(modid = RealElevator.MODID, value = Dist.CLIENT)
final class ClientClock {
    private ClientClock() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null) {
            ElevatorClock.tickClient(level, !minecraft.isPaused() && level.tickRateManager().runsNormally());
        }
    }
}
