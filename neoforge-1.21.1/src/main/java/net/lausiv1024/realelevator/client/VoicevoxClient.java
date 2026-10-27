package net.lausiv1024.realelevator.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.lausiv1024.realelevator.RealElevator;
import net.minecraft.client.Minecraft;

/**
 * ローカルで動いている VOICEVOX エンジンの HTTP API（audio_query → synthesis）で、文を WAV にする。
 *
 * <p>作った WAV は、話者と文ごとにメモリとディスク（{@code realelevator/voicevox_cache}）に覚えておき、同じ文は作り直さない。
 * 同じ文を作っているあいだに頼まれたら、同じ結果を待つ。
 */
final class VoicevoxClient {
    /** 1回の問い合わせを待つ長さ。 */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    /** 話者（スタイル）。credit はクレジットに出す名前（キャラクター名）。 */
    record Speaker(int id, String character, String style) {
    }

    private final Map<String, CompletableFuture<byte[]>> pending = new ConcurrentHashMap<>();

    /** エンジンの話者の一覧。 */
    static CompletableFuture<List<Speaker>> speakers(String engineUrl) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(trim(engineUrl) + "/speakers")).timeout(TIMEOUT).GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> {
            check(response.statusCode(), "speakers");
            List<Speaker> speakers = new ArrayList<>();
            for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonArray()) {
                JsonObject speaker = element.getAsJsonObject();
                String name = speaker.get("name").getAsString();
                JsonArray styles = speaker.getAsJsonArray("styles");
                for (JsonElement style : styles) {
                    JsonObject s = style.getAsJsonObject();
                    speakers.add(new Speaker(s.get("id").getAsInt(), name, s.get("name").getAsString()));
                }
            }
            return speakers;
        });
    }

    /** text を話者 speaker の声の WAV にする。覚えていればそれを返す。 */
    CompletableFuture<byte[]> synthesize(String engineUrl, int speaker, String text) {
        String key = speaker + "\\n" + text;
        return pending.computeIfAbsent(key, k -> {
            Path file = cacheFile(speaker, text);
            if (Files.isRegularFile(file)) {
                try {
                    return CompletableFuture.completedFuture(Files.readAllBytes(file));
                } catch (IOException e) {
                    RealElevator.LOGGER.warn("Couldn't read the VOICEVOX cache {}", file, e);
                }
            }
            CompletableFuture<byte[]> future = requestWav(engineUrl, speaker, text).thenApply(wav -> {
                try {
                    Files.createDirectories(file.getParent());
                    Files.write(file, wav);
                } catch (IOException e) {
                    RealElevator.LOGGER.warn("Couldn't write the VOICEVOX cache {}", file, e);
                }
                return wav;
            });
            // 失敗したものは覚えずに、次は問い合わせ直す
            future.whenComplete((wav, error) -> {
                if (error != null) {
                    pending.remove(k);
                }
            });
            return future;
        });
    }

    private static CompletableFuture<byte[]> requestWav(String engineUrl, int speaker, String text) {
        String base = trim(engineUrl);
        String query = "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) + "&speaker=" + speaker;
        HttpRequest audioQuery = HttpRequest.newBuilder(URI.create(base + "/audio_query" + query)).timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        return HTTP.sendAsync(audioQuery, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(response -> {
            check(response.statusCode(), "audio_query");
            HttpRequest synthesis = HttpRequest.newBuilder(URI.create(base + "/synthesis?speaker=" + speaker)).timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(response.body(), StandardCharsets.UTF_8)).build();
            return HTTP.sendAsync(synthesis, HttpResponse.BodyHandlers.ofByteArray());
        }).thenApply(response -> {
            check(response.statusCode(), "synthesis");
            return response.body();
        });
    }

    private static void check(int status, String what) {
        if (status / 100 != 2) {
            throw new IllegalStateException("VOICEVOX " + what + " returned " + status);
        }
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static Path cacheFile(int speaker, String text) {
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            hash = Integer.toHexString(text.hashCode());
        }
        return Minecraft.getInstance().gameDirectory.toPath().resolve("realelevator").resolve("voicevox_cache")
                .resolve(Integer.toString(speaker)).resolve(hash + ".wav");
    }
}
