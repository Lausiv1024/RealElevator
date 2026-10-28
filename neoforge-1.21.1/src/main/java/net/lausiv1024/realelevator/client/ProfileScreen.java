package net.lausiv1024.realelevator.client;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.design.ElevatorProfile;
import net.lausiv1024.realelevator.item.SpecificationItem;
import net.lausiv1024.realelevator.network.ProfilePayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 手元に保存したプロファイル（{@link ProfileStore}）の画面。保存したものの一覧と、今の設定に名前を付けて保存する欄を並べる。
 *
 * <ul>
 * <li>設置用ブロックの設定画面と保守用レンチのメニューから開くと、一覧から選んで今の設定に読み込める（保守用レンチでは、
 * 設置したあとも変えられる部分だけ）。今の設定を保存できる。</li>
 * <li>仕様書を持って何もないところを右クリックすると開き、一覧から選んで仕様書に書ける。仕様書の内容を保存できる。</li>
 * </ul>
 */
public final class ProfileScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int TOP = 32;
    private static final int ROW = 22;
    private static final int ACTION_WIDTH = 80;
    private static final int DELETE_WIDTH = 40;
    private static final int SAVE_WIDTH = 80;

    @Nullable
    private final Screen parent;
    /** 保存する内容。保存できなければ null（白紙の仕様書）。 */
    @Nullable
    private final CompoundTag current;
    /** 一覧から選んだプロファイルを受け取る。 */
    private final Consumer<CompoundTag> loader;
    private final Component actionLabel;
    private final Component hint;
    private List<ProfileStore.Entry> entries = List.of();
    private int scroll;
    private String name;
    @Nullable
    private Component status;

    private ProfileScreen(@Nullable Screen parent, @Nullable CompoundTag current, Consumer<CompoundTag> loader, Component actionLabel,
                          Component hint) {
        super(Component.translatable("gui.realelevator.profile.title"));
        this.parent = parent;
        this.current = current;
        this.loader = loader;
        this.actionLabel = actionLabel;
        this.hint = hint;
        this.name = current != null ? ElevatorProfile.name(current) : "";
    }

    /**
     * 設置用ブロックの設定画面か保守用レンチのメニューから開く。
     *
     * @param settings 今の設定（保存する内容）
     * @param loader   読み込んだプロファイルを受け取る。受け取ったあと parent に戻る
     * @param renewal  保守用レンチのメニューか（設置したあとも変えられる部分だけ読み込む）
     */
    static ProfileScreen forSettings(Screen parent, CompoundTag settings, Consumer<CompoundTag> loader, boolean renewal) {
        return new ProfileScreen(parent, ElevatorProfile.extract(settings), loader, Component.translatable("gui.realelevator.profile.load"),
                Component.translatable(renewal ? "gui.realelevator.profile.hint_renewal" : "gui.realelevator.profile.hint_installer"));
    }

    /** 手に持っている仕様書（hand）で開く。 */
    public static void openForSpecification(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        CompoundTag profile = SpecificationItem.profile(minecraft.player.getItemInHand(hand));
        minecraft.setScreen(new ProfileScreen(null, ElevatorProfile.isPresent(profile) ? profile : null,
                p -> PacketDistributor.sendToServer(new ProfilePayloads.Write(hand, p)),
                Component.translatable("gui.realelevator.profile.write"), Component.translatable("gui.realelevator.profile.hint_specification")));
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    /** 一覧に出せる行数。 */
    private int visibleRows() {
        return Math.max(1, (height - TOP - 100) / ROW);
    }

    @Override
    protected void init() {
        entries = ProfileStore.list();
        scroll = Math.max(0, Math.min(scroll, entries.size() - visibleRows()));
        int left = left();
        int rows = Math.min(visibleRows(), entries.size() - scroll);
        for (int i = 0; i < rows; i++) {
            ProfileStore.Entry entry = entries.get(scroll + i);
            int y = TOP + i * ROW;
            addRenderableWidget(Button.builder(actionLabel, b -> load(entry))
                    .bounds(left + WIDTH - ACTION_WIDTH - DELETE_WIDTH - 4, y, ACTION_WIDTH, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.profile.delete"), b -> confirmDelete(entry))
                    .bounds(left + WIDTH - DELETE_WIDTH, y, DELETE_WIDTH, 20).build());
        }

        int saveY = TOP + visibleRows() * ROW + 26;
        EditBox nameBox = new EditBox(font, left, saveY, WIDTH - SAVE_WIDTH - 4, 20, Component.translatable("gui.realelevator.profile.name"));
        nameBox.setMaxLength(ElevatorProfile.MAX_NAME_LENGTH);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("gui.realelevator.profile.name"));
        nameBox.setResponder(text -> name = text);
        nameBox.active = current != null;
        addRenderableWidget(nameBox);
        Button save = Button.builder(Component.translatable("gui.realelevator.profile.save"), b -> save())
                .bounds(left + WIDTH - SAVE_WIDTH, saveY, SAVE_WIDTH, 20).build();
        save.active = current != null;
        addRenderableWidget(save);
        addRenderableWidget(Button.builder(parent != null ? Component.translatable("gui.back") : Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 75, height - 28, 150, 20).build());
    }

    private void load(ProfileStore.Entry entry) {
        loader.accept(entry.profile());
        onClose();
    }

    private void save() {
        if (current == null || minecraft == null) {
            return;
        }
        String normalized = ProfileStore.normalize(name);
        if (normalized.isEmpty()) {
            status = Component.translatable("gui.realelevator.profile.need_name");
            return;
        }
        if (!ProfileStore.exists(normalized)) {
            store(normalized);
            return;
        }
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                store(normalized);
            }
            minecraft.setScreen(this);
        }, Component.translatable("gui.realelevator.profile.overwrite", normalized), Component.empty()));
    }

    private void store(String normalized) {
        status = Component.translatable(ProfileStore.save(normalized, current) ? "gui.realelevator.profile.saved" : "gui.realelevator.profile.failed",
                normalized);
        rebuildWidgets();
    }

    private void confirmDelete(ProfileStore.Entry entry) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                ProfileStore.delete(entry);
            }
            minecraft.setScreen(this);
        }, Component.translatable("gui.realelevator.profile.confirm_delete", entry.name()), Component.empty()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, entries.size() - visibleRows());
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        int left = left();
        int textWidth = WIDTH - ACTION_WIDTH - DELETE_WIDTH - 12;
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.realelevator.profile.empty"), width / 2, TOP + 6, 0xA0A0A0);
        }
        int rows = Math.min(visibleRows(), entries.size() - scroll);
        for (int i = 0; i < rows; i++) {
            ProfileStore.Entry entry = entries.get(scroll + i);
            int y = TOP + i * ROW;
            graphics.drawString(font, font.plainSubstrByWidth(entry.name(), textWidth), left, y + 1, 0xFFFFFF);
            graphics.drawString(font, font.substrByWidth(SpecificationItem.summary(entry.profile()), textWidth).getString(), left, y + 11, 0xA0A0A0);
        }
        if (entries.size() > visibleRows()) {
            graphics.drawCenteredString(font, Component.translatable("gui.realelevator.profile.scroll", scroll + 1,
                    Math.min(entries.size(), scroll + visibleRows()), entries.size()), width / 2, TOP + visibleRows() * ROW + 2, 0xA0A0A0);
        }
        int saveY = TOP + visibleRows() * ROW + 26;
        Component saveLabel = current != null
                ? Component.translatable("gui.realelevator.profile.current", SpecificationItem.summary(current))
                : Component.translatable("gui.realelevator.profile.nothing_to_save");
        graphics.drawString(font, saveLabel, left, saveY - 11, 0xE0E0E0);
        graphics.drawCenteredString(font, hint, width / 2, saveY + 26, 0xFFFF80);
        if (status != null) {
            graphics.drawCenteredString(font, status, width / 2, saveY + 38, 0xFFD080);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
