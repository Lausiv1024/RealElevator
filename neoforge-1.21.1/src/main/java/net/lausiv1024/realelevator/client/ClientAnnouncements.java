package net.lausiv1024.realelevator.client;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.network.AnnouncePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * サーバーから届いたアナウンスを鳴らす。今は同梱音声だけで、VOICEVOX 連携ができたら設定で切り替える。
 */
@EventBusSubscriber(modid = RealElevator.MODID, value = Dist.CLIENT)
public final class ClientAnnouncements {
    private static final AnnouncementPlayer PLAYER = new BundledAnnouncementPlayer();

    private ClientAnnouncements() {
    }

    public static void handle(AnnouncePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.carId()) instanceof ElevatorCarEntity car) {
            PLAYER.play(car, payload.announcement());
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        PLAYER.tick();
    }
}
