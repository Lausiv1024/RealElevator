package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.network.RenewalPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * エレベーターを管理できるプレイヤーの画面。設置者を出し、許可するプレイヤーを名前で追加、削除する。
 * 変えられるのは設置者とOPだけで、そのほかのプレイヤーには一覧だけを出す。
 */
public class AccessScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int TOP = 62;
    private static final int ROW = 22;
    private static final int REMOVE_WIDTH = 60;
    private static final int ADD_WIDTH = 60;

    private final Screen parent;
    private final int doorId;
    private String ownerName;
    private boolean canEdit;
    private List<String> allowed;
    private String typedName = "";
    private int scroll;

    AccessScreen(Screen parent, int doorId, CompoundTag access) {
        super(Component.translatable("gui.realelevator.access.title"));
        this.parent = parent;
        this.doorId = doorId;
        read(access);
    }

    /** サーバーから新しい一覧が届いたら、開いていれば出し直す。 */
    public static void update(RenewalPayloads.AccessList payload) {
        if (Minecraft.getInstance().screen instanceof AccessScreen screen && screen.doorId == payload.doorId()) {
            screen.read(payload.access());
            screen.rebuildWidgets();
        }
    }

    private void read(CompoundTag access) {
        ownerName = access.getString("OwnerName");
        canEdit = access.getBoolean("CanEdit");
        allowed = new ArrayList<>();
        ListTag names = access.getList("Allowed", Tag.TAG_STRING);
        for (int i = 0; i < names.size(); i++) {
            allowed.add(names.getString(i));
        }
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int visibleRows() {
        return Math.max(1, (height - TOP - 40) / ROW);
    }

    @Override
    protected void init() {
        scroll = Math.max(0, Math.min(scroll, allowed.size() - visibleRows()));
        int left = left();
        if (canEdit) {
            EditBox name = new EditBox(font, left, 34, WIDTH - ADD_WIDTH - 4, 20, Component.translatable("gui.realelevator.access.name"));
            name.setMaxLength(16);
            name.setValue(typedName);
            name.setResponder(value -> typedName = value);
            name.setHint(Component.translatable("gui.realelevator.access.name"));
            addRenderableWidget(name);
            addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.access.add"), b -> {
                if (!typedName.isBlank()) {
                    PacketDistributor.sendToServer(new RenewalPayloads.AccessEdit(doorId, true, typedName.trim()));
                    typedName = "";
                    name.setValue("");
                }
            }).bounds(left + WIDTH - ADD_WIDTH, 34, ADD_WIDTH, 20).build());
            int rows = Math.min(visibleRows(), allowed.size() - scroll);
            for (int i = 0; i < rows; i++) {
                String player = allowed.get(scroll + i);
                addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.access.remove"),
                                b -> PacketDistributor.sendToServer(new RenewalPayloads.AccessEdit(doorId, false, player)))
                        .bounds(left + WIDTH - REMOVE_WIDTH, TOP + i * ROW, REMOVE_WIDTH, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, allowed.size() - visibleRows());
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
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.realelevator.access.owner", ownerName), width / 2, 20, 0xFFFF80);
        int left = left();
        int rows = Math.min(visibleRows(), allowed.size() - scroll);
        for (int i = 0; i < rows; i++) {
            graphics.drawString(font, allowed.get(scroll + i), left, TOP + i * ROW + 6, 0xFFFFFF);
        }
        if (allowed.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.realelevator.access.none"), width / 2, TOP + 6, 0xA0A0A0);
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
