package net.lausiv1024.realelevator;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * クライアントのコンフィグ（{@code config/realelevator-client.toml}）。VOICEVOX 連携の設定を持つ。
 * モッド一覧の「設定」から開く画面（{@code VoicevoxScreen}）でも変えられる。
 */
public final class RealElevatorClientConfig {
    public static final ModConfigSpec SPEC;
    /** VOICEVOX でアナウンスを読み上げるか。エンジンが動いていなければ同梱音声に戻る。 */
    public static final ModConfigSpec.BooleanValue VOICEVOX_ENABLED;
    /** VOICEVOX エンジンの URL。 */
    public static final ModConfigSpec.ConfigValue<String> ENGINE_URL;
    /** 話者（スタイル）の番号。 */
    public static final ModConfigSpec.IntValue SPEAKER;
    /** 話者の名前（クレジットの表示に使う）。 */
    public static final ModConfigSpec.ConfigValue<String> SPEAKER_NAME;

    /** 同梱音声と同じ、冥鳴ひまり（ノーマル）。 */
    public static final int DEFAULT_SPEAKER = 14;
    public static final String DEFAULT_SPEAKER_NAME = "冥鳴ひまり";
    public static final String DEFAULT_ENGINE_URL = "http://127.0.0.1:50021";

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("voicevox");
        VOICEVOX_ENABLED = builder.comment("Read announcements with a local VOICEVOX engine (falls back to the bundled voice when it is not running).")
                .define("enabled", false);
        ENGINE_URL = builder.comment("URL of the VOICEVOX engine.").define("engineUrl", DEFAULT_ENGINE_URL);
        SPEAKER = builder.comment("Speaker style id.").defineInRange("speaker", DEFAULT_SPEAKER, 0, Integer.MAX_VALUE);
        SPEAKER_NAME = builder.comment("Speaker name, shown as the credit.").define("speakerName", DEFAULT_SPEAKER_NAME);
        builder.pop();
        SPEC = builder.build();
    }

    private RealElevatorClientConfig() {
    }

    /** VOICEVOX を使うか。コンフィグがまだ読み込まれていなければ使わない。 */
    public static boolean voicevoxEnabled() {
        return SPEC.isLoaded() && VOICEVOX_ENABLED.get();
    }
}
