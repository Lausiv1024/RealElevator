package net.lausiv1024.realelevator.client;

import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.announce.AnnouncementScript;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 同梱音声でアナウンスを鳴らす。録っておいたクリップ（{@link AnnouncementScript#clips}）を、
 * 1つ鳴り終わるごとに次を鳴らしてつなげる。音はかごの中（床から2.5ブロック）から鳴らす。
 *
 * <p>クリップの長さは、音声を作ったときに書き出した assets/realelevator/announcements.json から読む。
 */
public final class BundledAnnouncementPlayer implements AnnouncementPlayer {
    private static final ResourceLocation DURATIONS = ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "announcements.json");
    /** 長さが分からないクリップの長さ [秒]。 */
    private static final double DEFAULT_SECONDS = 1.5;
    private static final double SPEAKER_HEIGHT = 2.5;

    private record Clip(String name, int carId) {
    }

    private final Map<Integer, Deque<Clip>> queues = new HashMap<>();
    private final Map<Integer, SoundInstance> playing = new HashMap<>();
    private final Map<Integer, Long> nextAt = new HashMap<>();
    @Nullable
    private Map<String, Double> durations;

    @Override
    public void play(ElevatorCarEntity car, Announcement announcement) {
        stop(car.getId());
        Deque<Clip> queue = new ArrayDeque<>();
        for (String clip : AnnouncementScript.clips(announcement)) {
            queue.add(new Clip(clip, car.getId()));
        }
        queues.put(car.getId(), queue);
        nextAt.put(car.getId(), Long.MIN_VALUE);
        tick();
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            queues.clear();
            playing.clear();
            nextAt.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        queues.entrySet().removeIf(entry -> {
            int carId = entry.getKey();
            Deque<Clip> queue = entry.getValue();
            if (queue.isEmpty()) {
                nextAt.remove(carId);
                return true;
            }
            if (now < nextAt.getOrDefault(carId, Long.MIN_VALUE)) {
                return false;
            }
            if (!(minecraft.level.getEntity(carId) instanceof ElevatorCarEntity car)) {
                nextAt.remove(carId);
                return true;
            }
            Clip clip = queue.poll();
            SoundInstance sound = new SimpleSoundInstance(
                    ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "announce." + clip.name()),
                    SoundSource.VOICE, 1.0F, 1.0F, RandomSource.create(), false, 0, SoundInstance.Attenuation.LINEAR,
                    car.getX(), car.getY() + SPEAKER_HEIGHT, car.getZ(), false);
            minecraft.getSoundManager().play(sound);
            playing.put(carId, sound);
            nextAt.put(carId, now + Math.round(duration(clip.name()) * 20));
            return false;
        });
    }

    private void stop(int carId) {
        SoundInstance sound = playing.remove(carId);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
        queues.remove(carId);
        nextAt.remove(carId);
    }

    private double duration(String clip) {
        if (durations == null) {
            durations = loadDurations();
        }
        return durations.getOrDefault(clip, DEFAULT_SECONDS);
    }

    private static Map<String, Double> loadDurations() {
        Map<String, Double> result = new HashMap<>();
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(DURATIONS);
        if (resource.isEmpty()) {
            return result;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("durations");
            json.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsDouble()));
        } catch (Exception e) {
            RealElevator.LOGGER.warn("Failed to read announcement durations", e);
        }
        return result;
    }
}
