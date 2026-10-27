package net.lausiv1024.realelevator.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.RealElevatorClientConfig;
import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.announce.AnnouncementScript;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.minecraft.client.Minecraft;

/**
 * VOICEVOX でアナウンスを鳴らす。文（{@link AnnouncementScript#text}）を、ローカルの VOICEVOX エンジンで WAV にして、かごの中
 * （床から2.5ブロック）から鳴らす。同じかごで別のアナウンスが鳴っている（または作っている）あいだに届いたものは捨てる。
 *
 * <p>エンジンにつながらなかったり、作るのに時間がかかりすぎたりしたら、そのアナウンスは同梱音声で鳴らし、しばらく
 * （{@value #RETRY_TICKS} tick）はエンジンに問い合わせずに同梱音声を使う。初めての文は作るのに少しかかるので、
 * 読み込まれているかごの停止階の到着の文と、行先方向・戸閉の文は、前もって作っておく。
 */
final class VoicevoxAnnouncementPlayer implements AnnouncementPlayer {
    /** 作るのを待つ長さ [tick]。これより遅れたアナウンスは、間が抜けるので同梱音声で鳴らす。 */
    private static final int MAX_DELAY_TICKS = 30;
    /** エンジンにつながらなかったあと、同梱音声を使う長さ [tick]。 */
    private static final int RETRY_TICKS = 600;
    /** 前もって作る文を探す間隔 [tick]。 */
    private static final int PREFETCH_INTERVAL = 100;
    private static final double SPEAKER_HEIGHT = 2.5;

    private final VoicevoxClient client = new VoicevoxClient();
    private final AnnouncementPlayer fallback;
    /** かごごとの、作っている（鳴らす前の）アナウンス。 */
    private final Set<Integer> synthesizing = new HashSet<>();
    private final Map<Integer, PcmSoundInstance> playing = new HashMap<>();
    private long engineDownUntil = Long.MIN_VALUE;
    private long nextPrefetch;

    VoicevoxAnnouncementPlayer(AnnouncementPlayer fallback) {
        this.fallback = fallback;
    }

    @Override
    public void play(ElevatorCarEntity car, Announcement announcement) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (now < engineDownUntil) {
            fallback.play(car, announcement);
            return;
        }
        int carId = car.getId();
        if (isBusy(carId)) {
            // 別のアナウンスが鳴っているので、流し直さずに捨てる
            return;
        }
        String text = AnnouncementScript.text(announcement);
        if (text.isEmpty()) {
            return;
        }
        synthesizing.add(carId);
        client.synthesize(RealElevatorClientConfig.ENGINE_URL.get(), RealElevatorClientConfig.SPEAKER.get(), text)
                .whenComplete((wav, error) -> minecraft.execute(() -> {
                    synthesizing.remove(carId);
                    if (minecraft.level == null || !(minecraft.level.getEntity(carId) instanceof ElevatorCarEntity target)) {
                        return;
                    }
                    boolean late = minecraft.level.getGameTime() - now > MAX_DELAY_TICKS;
                    if (error != null) {
                        RealElevator.LOGGER.warn("VOICEVOX is not available, using the bundled voice: {}", error.toString());
                        engineDownUntil = minecraft.level.getGameTime() + RETRY_TICKS;
                    }
                    if (error != null || late) {
                        fallback.play(target, announcement);
                        return;
                    }
                    try {
                        PcmSoundInstance sound = PcmSoundInstance.fromWav(wav, target.getX(), target.getY() + SPEAKER_HEIGHT, target.getZ());
                        minecraft.getSoundManager().play(sound);
                        playing.put(carId, sound);
                    } catch (Exception e) {
                        RealElevator.LOGGER.warn("Couldn't play the VOICEVOX voice", e);
                        fallback.play(target, announcement);
                    }
                }));
    }

    @Override
    public void tick() {
        fallback.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            playing.clear();
            synthesizing.clear();
            return;
        }
        playing.values().removeIf(sound -> !minecraft.getSoundManager().isActive(sound));
        long now = minecraft.level.getGameTime();
        if (RealElevatorClientConfig.voicevoxEnabled() && now >= nextPrefetch && now >= engineDownUntil) {
            nextPrefetch = now + PREFETCH_INTERVAL;
            prefetch();
        }
    }

    /** 読み込まれているかごで鳴らしそうな文を、前もって作っておく（覚えていれば何もしない）。 */
    private void prefetch() {
        Minecraft minecraft = Minecraft.getInstance();
        String url = RealElevatorClientConfig.ENGINE_URL.get();
        int speaker = RealElevatorClientConfig.SPEAKER.get();
        Set<String> texts = new HashSet<>();
        texts.add(AnnouncementScript.text(Announcement.direction(TravelDirection.UP)));
        texts.add(AnnouncementScript.text(Announcement.direction(TravelDirection.DOWN)));
        texts.add(AnnouncementScript.text(Announcement.doorClose()));
        for (ElevatorCarEntity car : ElevatorTracker.cars(minecraft.level)) {
            for (int floor = 0; floor < car.floorCount(); floor++) {
                texts.add(AnnouncementScript.text(Announcement.arrival(car.floorLabel(floor), car.floorSpeechName(floor))));
            }
        }
        for (String text : texts) {
            if (!text.isEmpty()) {
                // エンジンが動いていなければ、しばらく問い合わせない
                client.synthesize(url, speaker, text).exceptionally(error -> {
                    minecraft.execute(() -> {
                        if (minecraft.level != null) {
                            engineDownUntil = minecraft.level.getGameTime() + RETRY_TICKS;
                        }
                    });
                    return null;
                });
            }
        }
    }

    private boolean isBusy(int carId) {
        if (synthesizing.contains(carId)) {
            return true;
        }
        PcmSoundInstance sound = playing.get(carId);
        return sound != null && Minecraft.getInstance().getSoundManager().isActive(sound);
    }
}
