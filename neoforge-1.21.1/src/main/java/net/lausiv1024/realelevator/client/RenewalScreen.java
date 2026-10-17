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
 * 保守用レンチのメニュー。設置したあとに変えられる設定（配置・アナウンス・階名）を変え（リニューアル）、エレベーターを解体する。
 * 乗場の操作盤をレンチで右クリックすると、サーバーが頂部の制御盤の設定を送ってきて開く。
 */
public class RenewalScreen extends Screen {
    private final int doorId;
    private final SettingsForm form;

    private RenewalScreen(int doorId, CompoundTag settings) {
        super(Component.translatable("gui.realelevator.renewal.title"));
        this.doorId = doorId;
        this.form = new SettingsForm(settings);
    }

    /** サーバーから {@link RenewalPayloads.Show} が届いたら開く。 */
    public static void open(RenewalPayloads.Show payload) {
        Minecraft.getInstance().setScreen(new RenewalScreen(payload.doorId(), payload.settings()));
    }

    @Override
    protected void init() {
        List<AbstractWidget> settings = form.fixtureWidgets(this::send);
        form.layout(font, width, height - 42, settings, this::addRenderableWidget, this::send, this::rebuildWidgets);

        int bottom = height - 26;
        int gap = SettingsForm.GAP;
        int buttonWidth = Math.min(130, (width - SettingsForm.MARGIN * 2 - gap) / 2);
        int buttonsLeft = (width - buttonWidth * 2 - gap) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.renewal.dismantle"), b -> confirmDismantle())
                .bounds(buttonsLeft, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(buttonsLeft + buttonWidth + gap, bottom, buttonWidth, 20).build());
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

    /** 今の設定をサーバーへ送る。 */
    private void send() {
        CompoundTag tag = new CompoundTag();
        form.writeTo(tag);
        PacketDistributor.sendToServer(new RenewalPayloads.Apply(doorId, tag));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        form.renderFloors(graphics, font);
        graphics.drawCenteredString(font, Component.translatable("gui.realelevator.renewal.hint"), width / 2, height - 38, 0xFFFF80);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
