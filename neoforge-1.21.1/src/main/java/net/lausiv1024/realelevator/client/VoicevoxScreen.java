package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.RealElevatorClientConfig;
import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.announce.AnnouncementScript;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * VOICEVOX 連携の設定画面。モッド一覧の「設定」から開く。使うかどうか、エンジンの URL、話者を選び、試しに読ませられる。
 * 話者はエンジンから一覧を読み込んで選ぶ。選んだ話者のクレジット（「VOICEVOX:キャラクター名」）を出す。
 */
public final class VoicevoxScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int ROW = 24;
    /** 試しに読む文。 */
    private static final Announcement SAMPLE = Announcement.arrival("1");

    private final Screen parent;
    private final VoicevoxClient client = new VoicevoxClient();
    private boolean enabled;
    private String url;
    private int speaker;
    private String speakerName;
    private List<VoicevoxClient.Speaker> speakers = List.of();
    @Nullable
    private Component status;
    @Nullable
    private EditBox urlBox;

    public VoicevoxScreen(Screen parent) {
        super(Component.translatable("gui.realelevator.voicevox.title"));
        this.parent = parent;
        this.enabled = RealElevatorClientConfig.VOICEVOX_ENABLED.get();
        this.url = RealElevatorClientConfig.ENGINE_URL.get();
        this.speaker = RealElevatorClientConfig.SPEAKER.get();
        this.speakerName = RealElevatorClientConfig.SPEAKER_NAME.get();
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int y = 40;
        addRenderableWidget(CycleButton.onOffBuilder(enabled)
                .create(left, y, WIDTH, 20, Component.translatable("gui.realelevator.voicevox.enabled"), (b, v) -> enabled = v));
        y += ROW;
        urlBox = new EditBox(font, left, y, WIDTH - 84, 20, Component.translatable("gui.realelevator.voicevox.url"));
        urlBox.setMaxLength(200);
        urlBox.setValue(url);
        urlBox.setResponder(text -> url = text);
        addRenderableWidget(urlBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.voicevox.load"), b -> loadSpeakers())
                .bounds(left + WIDTH - 80, y, 80, 20).build());
        y += ROW;
        if (speakers.isEmpty()) {
            Button current = Button.builder(Component.translatable("gui.realelevator.voicevox.speaker_current", speakerName, speaker), b -> {
            }).bounds(left, y, WIDTH, 20).build();
            current.active = false;
            addRenderableWidget(current);
        } else {
            VoicevoxClient.Speaker initial = speakers.stream().filter(s -> s.id() == speaker).findFirst().orElse(speakers.get(0));
            select(initial);
            addRenderableWidget(CycleButton.<VoicevoxClient.Speaker>builder(s -> Component.literal(s.character() + "（" + s.style() + "）"))
                    .withValues(speakers).withInitialValue(initial)
                    .create(left, y, WIDTH, 20, Component.translatable("gui.realelevator.voicevox.speaker"), (b, v) -> select(v)));
        }
        y += ROW;
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.voicevox.test"), b -> test())
                .bounds(left, y, WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 75, height - 28, 150, 20).build());
    }

    private void select(VoicevoxClient.Speaker value) {
        speaker = value.id();
        speakerName = value.character();
    }

    private void loadSpeakers() {
        status = Component.translatable("gui.realelevator.voicevox.loading");
        VoicevoxClient.speakers(url).whenComplete((list, error) -> {
            if (minecraft == null) {
                return;
            }
            minecraft.execute(() -> {
                if (error != null || list.isEmpty()) {
                    status = Component.translatable("gui.realelevator.voicevox.failed");
                    return;
                }
                speakers = new ArrayList<>(list);
                status = Component.translatable("gui.realelevator.voicevox.loaded", list.size());
                rebuildWidgets();
            });
        });
    }

    /** 選んでいる話者で、試しに「1階です。」を読ませる（自分の位置で鳴らす）。 */
    private void test() {
        if (minecraft == null) {
            return;
        }
        status = Component.translatable("gui.realelevator.voicevox.loading");
        client.synthesize(url, speaker, AnnouncementScript.text(SAMPLE)).whenComplete((wav, error) -> minecraft.execute(() -> {
            if (error != null) {
                status = Component.translatable("gui.realelevator.voicevox.failed");
                return;
            }
            try {
                double x = minecraft.player != null ? minecraft.player.getX() : 0;
                double y = minecraft.player != null ? minecraft.player.getEyeY() : 0;
                double z = minecraft.player != null ? minecraft.player.getZ() : 0;
                minecraft.getSoundManager().play(PcmSoundInstance.fromWav(wav, x, y, z));
                status = null;
            } catch (Exception e) {
                status = Component.translatable("gui.realelevator.voicevox.failed");
            }
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        int y = 40 + ROW * 4 + 6;
        // クレジット：キャラクターごとの利用規約で、使うときに表記が必要
        graphics.drawCenteredString(font, Component.translatable("gui.realelevator.voicevox.credit", speakerName), width / 2, y, 0xE0E0E0);
        graphics.drawCenteredString(font, Component.translatable("gui.realelevator.voicevox.terms"), width / 2, y + 12, 0xA0A0A0);
        if (status != null) {
            graphics.drawCenteredString(font, status, width / 2, y + 30, 0xFFD080);
        }
    }

    @Override
    public void onClose() {
        RealElevatorClientConfig.VOICEVOX_ENABLED.set(enabled);
        RealElevatorClientConfig.ENGINE_URL.set(url);
        RealElevatorClientConfig.SPEAKER.set(speaker);
        RealElevatorClientConfig.SPEAKER_NAME.set(speakerName);
        RealElevatorClientConfig.SPEC.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
