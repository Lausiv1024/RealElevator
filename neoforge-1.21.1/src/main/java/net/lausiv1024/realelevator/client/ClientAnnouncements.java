package net.lausiv1024.realelevator.client;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.RealElevatorClientConfig;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.network.AnnouncePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * サーバーから届いたアナウンスを鳴らす。クライアントのコンフィグで VOICEVOX がオンなら VOICEVOX で（エンジンが動いていなければ
 * 同梱音声で）、オフなら同梱音声で鳴らす。
 */
@EventBusSubscriber(modid = RealElevator.MODID, value = Dist.CLIENT)
public final class ClientAnnouncements {
    private static final AnnouncementPlayer BUNDLED = new BundledAnnouncementPlayer();
    private static final AnnouncementPlayer VOICEVOX = new VoicevoxAnnouncementPlayer(BUNDLED);

    private ClientAnnouncements() {
    }

    public static void handle(AnnouncePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.carId()) instanceof ElevatorCarEntity car) {
            (RealElevatorClientConfig.voicevoxEnabled() ? VOICEVOX : BUNDLED).play(car, payload.announcement());
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        // VOICEVOX の再生は、代わりに使う同梱音声の続きも進める
        VOICEVOX.tick();
    }
}
