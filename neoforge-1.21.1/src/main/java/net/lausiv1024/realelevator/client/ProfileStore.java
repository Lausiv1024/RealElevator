package net.lausiv1024.realelevator.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.design.ElevatorProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * 手元に保存したプロファイル（{@link ElevatorProfile}）。{@code realelevator/profiles/<名前>.json} に、プロファイルを JSON にして
 * 1つずつ置く。どのワールド・サーバーでも使える。ファイル名は名前からファイルに使えない文字を除いたもの。
 */
final class ProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String EXTENSION = ".json";

    /** 保存したプロファイル。name は表示する名前、profile は名前を付けたプロファイル、file は置いてあるファイル。 */
    record Entry(String name, CompoundTag profile, Path file) {
    }

    private ProfileStore() {
    }

    private static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("realelevator").resolve("profiles");
    }

    /** 保存したプロファイルを名前の順に。読めないファイルは飛ばす。 */
    static List<Entry> list() {
        List<Entry> entries = new ArrayList<>();
        Path dir = directory();
        if (!Files.isDirectory(dir)) {
            return entries;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*" + EXTENSION)) {
            for (Path file : files) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    Tag tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, GSON.fromJson(reader, JsonElement.class));
                    if (tag instanceof CompoundTag profile && ElevatorProfile.isPresent(profile)) {
                        String fileName = file.getFileName().toString();
                        String name = ElevatorProfile.name(profile);
                        if (name.isEmpty()) {
                            name = fileName.substring(0, fileName.length() - EXTENSION.length());
                        }
                        entries.add(new Entry(name, ElevatorProfile.withName(ElevatorProfile.sanitize(profile), name), file));
                    }
                } catch (IOException | RuntimeException e) {
                    RealElevator.LOGGER.warn("Couldn't read the elevator profile {}", file, e);
                }
            }
        } catch (IOException e) {
            RealElevator.LOGGER.warn("Couldn't list the elevator profiles", e);
        }
        entries.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    /** 名前 name で保存したものがあるか。 */
    static boolean exists(String name) {
        return Files.exists(file(name));
    }

    /** プロファイル profile を名前 name で保存する（同じ名前があれば上書き）。保存できたか。 */
    static boolean save(String name, CompoundTag profile) {
        Path file = file(name);
        try {
            Files.createDirectories(file.getParent());
            JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, ElevatorProfile.withName(ElevatorProfile.extract(profile), name));
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            RealElevator.LOGGER.warn("Couldn't save the elevator profile {}", file, e);
            return false;
        }
    }

    /** 保存したプロファイル entry を消す。 */
    static void delete(Entry entry) {
        try {
            Files.deleteIfExists(entry.file());
        } catch (IOException e) {
            RealElevator.LOGGER.warn("Couldn't delete the elevator profile {}", entry.file(), e);
        }
    }

    /** 名前 name を保存できる名前にしたもの（前後の空白を除き、長さを切る）。空なら保存できない。 */
    static String normalize(String name) {
        return ElevatorProfile.name(ElevatorProfile.withName(new CompoundTag(), name));
    }

    private static Path file(String name) {
        StringBuilder safe = new StringBuilder();
        normalize(name).codePoints().forEach(c -> safe.appendCodePoint(c < 0x20 || "\\/:*?\"<>|".indexOf(c) >= 0 ? '_' : c));
        // 「.」で終わる名前は Windows で扱えない
        String fileName = safe.toString().replaceAll("\\.+$", "_");
        return directory().resolve(fileName + EXTENSION);
    }
}
