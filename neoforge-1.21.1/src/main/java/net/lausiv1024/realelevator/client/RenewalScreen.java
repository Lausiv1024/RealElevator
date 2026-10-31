package net.lausiv1024.realelevator.client;

import java.util.List;

import net.lausiv1024.realelevator.network.RenewalPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 保守用レンチのメニュー。設置したあとに変えられる設定（配置・アナウンス・階名）を変え（リニューアル）、群管理の群に入れたり外したりし、
 * エレベーターを解体する。群に入れるときは、しゃがみながらレンチで選んでおいたエレベーターと同じ群にする。
 * 乗場の操作盤をレンチで右クリックすると、サーバーが頂部の制御盤の設定を送ってきて開く。
 */
public class RenewalScreen extends Screen {
    private final int doorId;
    private final SettingsForm form;
    /** サーバーから届いた設定（機種・速度などを含む）。プロファイルを保存するときに使う。 */
    private final CompoundTag settings;
    /** 群管理の群の台数。群に入っていなければ0。 */
    private final int groupSize;

    private RenewalScreen(int doorId, CompoundTag settings) {
        super(Component.translatable("gui.realelevator.renewal.title"));
        this.doorId = doorId;
        this.form = new SettingsForm(settings);
        this.settings = settings;
        this.groupSize = settings.getInt("GroupSize");
    }

    /** サーバーから {@link RenewalPayloads.Show} が届いたら開く。 */
    public static void open(RenewalPayloads.Show payload) {
        Minecraft.getInstance().setScreen(new RenewalScreen(payload.doorId(), payload.settings()));
    }

    @Override
    protected void init() {
        List<AbstractWidget> settings = form.fixtureWidgets(this::send, this::rebuildWidgets);
        form.layout(font, width, height - 42, settings, this::addRenderableWidget, this::send, this::rebuildWidgets);

        int bottom = height - 26;
        int gap = SettingsForm.GAP;
        int buttonWidth = Math.min(100, (width - SettingsForm.MARGIN * 2 - gap * 5) / 6);
        int buttonsLeft = (width - buttonWidth * 6 - gap * 5) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.renewal.dismantle"), b -> confirmDismantle())
                .bounds(buttonsLeft, bottom, buttonWidth, 20).build());
        // 群の入れ外しは結果をチャットに出すので、画面を閉じる（開き直すと台数が新しくなる）
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.renewal.group_join"), b -> {
            PacketDistributor.sendToServer(new RenewalPayloads.Group(doorId, true));
            onClose();
        }).bounds(buttonsLeft + buttonWidth + gap, bottom, buttonWidth, 20).build());
        Button leave = Button.builder(Component.translatable("gui.realelevator.renewal.group_leave"), b -> {
            PacketDistributor.sendToServer(new RenewalPayloads.Group(doorId, false));
            onClose();
        }).bounds(buttonsLeft + (buttonWidth + gap) * 2, bottom, buttonWidth, 20).build();
        leave.active = groupSize > 0;
        addRenderableWidget(leave);
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.profile"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(ProfileScreen.forSettings(this, currentSettings(), profile -> {
                    form.applyProfile(profile);
                    send();
                }, true));
            }
        }).bounds(buttonsLeft + (buttonWidth + gap) * 3, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.access.open"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new AccessScreen(this, doorId, this.settings.getCompound("Access")));
            }
        }).bounds(buttonsLeft + (buttonWidth + gap) * 4, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(buttonsLeft + (buttonWidth + gap) * 5, bottom, buttonWidth, 20).build());
    }

    private void confirmDismantle() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                PacketDistributor.sendToServer(new RenewalPayloads.Dismantle(doorId));
                onClose();
            } else {
                minecraft.setScreen(this);
            }
        }, Component.translatable("gui.realelevator.renewal.confirm.title"),
                Component.translatable("gui.realelevator.renewal.confirm.message")));
    }

    /** 今の設定（機種・速度などは届いたまま）。 */
    private CompoundTag currentSettings() {
        CompoundTag tag = settings.copy();
        form.writeTo(tag);
        return tag;
    }

    /** 今の設定をサーバーへ送る。 */
    private void send() {
        CompoundTag tag = new CompoundTag();
        form.writeTo(tag);
        PacketDistributor.sendToServer(new RenewalPayloads.Apply(doorId, tag));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Component heading = groupSize > 0 ? title.copy().append(Component.translatable("gui.realelevator.renewal.group_size", groupSize)) : title;
        graphics.drawCenteredString(font, heading, width / 2, 10, 0xFFFFFF);
        form.renderFloors(graphics, font);
        graphics.drawCenteredString(font, Component.translatable("gui.realelevator.renewal.hint"), width / 2, height - 38, 0xFFFF80);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
