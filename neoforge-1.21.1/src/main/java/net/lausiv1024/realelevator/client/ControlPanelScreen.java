package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import net.lausiv1024.realelevator.block.ControlPanelBlockEntity;
import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.install.ShaftLayout.FloorProblem;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.lausiv1024.realelevator.menu.ControlPanelMenu;
import net.lausiv1024.realelevator.network.PanelPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 制御盤の設定画面。「設定」と「動力」の2つの表示を、下のボタンで切り替える。
 *
 * <ul>
 * <li>設定：左に機種・速度・ピット・配置・アナウンスの設定、右に停止階と階名、下にプレビューと確定を並べる。
 * 画面の高さに収まる行数を求め、設定のボタンは足りない分だけ列を増やして折り返し、停止階は収まる行数だけを出してスクロールする。</li>
 * <li>動力：ネザースターのスロットと持ち物。ネザースターをはめると、高速仕様を選べるようになる。</li>
 * </ul>
 *
 * 設定を変えるたびにサーバーへ送り、サーバーの制御盤が反映して送り返す。
 */
public class ControlPanelScreen extends AbstractContainerScreen<ControlPanelMenu> {
    private static final int ROW = 22;
    private static final int TOP = 28;
    private static final int MARGIN = 10;
    private static final int GAP = 4;
    /** 設定のボタンの幅の上限。 */
    private static final int MAX_SETTING_WIDTH = 200;
    /** 停止階の欄の幅（「◯階（Y=◯）」、階名の入力欄、スクロールのボタン）。 */
    private static final int FLOOR_COLUMN_WIDTH = 170;
    /** 邪魔なブロックの数え直しの間隔 [フレーム]。 */
    private static final int OBSTRUCTION_REFRESH = 20;

    // 「動力」の表示の枠の色（バニラのコンテナの画面に似せる）
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int LABEL = 0x404040;

    private CarModel model;
    private SpeedSetting speed;
    private PitMode pitMode;
    private FixtureConfig fixtures;
    private AnnouncementConfig announcements;
    private boolean preview;
    /** 「動力」の表示か。 */
    private boolean showPower;
    /** 前に見たときにネザースターがはまっていたか。変わったら、選べる速度が変わるので作り直す。 */
    private boolean hadStar;
    private final List<EditBox> labelBoxes = new ArrayList<>();
    private int[] floors = new int[0];
    private int scroll;
    /** 停止階を一度に出す行数。画面の高さで決まる。 */
    private int visibleFloors;
    /** 停止階の欄の左端。 */
    private int floorColumnX;
    private int obstructionCount;
    private int frames;

    public ControlPanelScreen(ControlPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = ControlPanelMenu.INVENTORY_Y - 11;
    }

    @Nullable
    private ControlPanelBlockEntity panel() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.pos()) instanceof ControlPanelBlockEntity panel ? panel : null;
    }

    @Override
    protected void init() {
        super.init();
        ControlPanelBlockEntity panel = panel();
        if (panel == null) {
            onClose();
            return;
        }
        if (model == null) {
            model = panel.model();
            speed = panel.speed();
            pitMode = panel.pitMode();
            fixtures = panel.fixtures();
            announcements = panel.announcements();
            preview = panel.isPreviewing();
            hadStar = panel.hasNetherStar();
        }
        menu.setSlotsVisible(showPower);
        if (showPower) {
            initPower();
        } else {
            initSettings(panel);
        }
    }

    private void initPower() {
        int bottom = topPos + imageHeight + 6;
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.back"), b -> switchView(false))
                .bounds(leftPos, bottom, 86, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(leftPos + imageWidth - 86, bottom, 86, 20).build());
    }

    private void initSettings(ControlPanelBlockEntity panel) {
        floors = panel.floorYs();
        boolean locked = panel.isInstalled();
        int statusY = height - 42;
        int rowsFit = Math.max(1, (statusY - 4 - TOP) / ROW);
        visibleFloors = rowsFit;

        List<AbstractWidget> settings = new ArrayList<>();
        CycleButton<CarModel> modelButton = CycleButton.<CarModel>builder(m -> Component.literal(m.name().replace("P", "P-")))
                .withValues(CarModel.values()).withInitialValue(model)
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.model"), (b, v) -> {
                    model = v;
                    send();
                });
        modelButton.active = !locked;
        settings.add(modelButton);
        // 高速仕様は、ネザースターがはまっているとき（または高速仕様で設置したあと）だけ出す
        List<SpeedSpec> speeds = new ArrayList<>();
        for (SpeedSpec spec : SpeedSpec.values()) {
            if (!spec.isHighSpeed() || panel.hasNetherStar() || spec == speed.spec()) {
                speeds.add(spec);
            }
        }
        CycleButton<SpeedSpec> speedButton = CycleButton.<SpeedSpec>builder(sp -> Component.translatable(
                        "gui.realelevator.speed." + sp.name().toLowerCase(Locale.ROOT)))
                .withValues(speeds).withInitialValue(speed.spec())
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.speed"), (b, v) -> {
                    speed = speed.withSpec(v);
                    send();
                    // 加速度とジャークを動かせるかが変わる
                    rebuildWidgets();
                });
        speedButton.active = !locked;
        settings.add(speedButton);
        boolean adjustable = !locked && speed.isAdjustable();
        settings.add(new ValueSlider("acceleration", speed.acceleration(), speed.minAcceleration(), SpeedSetting.MAX_ACCELERATION,
                adjustable, v -> speed = speed.withAcceleration(v)));
        settings.add(new ValueSlider("jerk", speed.jerk(), speed.minJerk(), SpeedSetting.MAX_JERK,
                adjustable, v -> speed = speed.withJerk(v)));
        CycleButton<PitMode> pitButton = CycleButton.<PitMode>builder(p -> Component.translatable(
                        "gui.realelevator.pit." + p.name().toLowerCase(Locale.ROOT)))
                .withValues(PitMode.values()).withInitialValue(pitMode)
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.pit"), (b, v) -> {
                    pitMode = v;
                    send();
                });
        pitButton.active = !locked;
        settings.add(pitButton);
        settings.add(enumButton("panel_sides", FixtureConfig.PanelSides.values(), fixtures.panelSides(), v -> fixtures = fixtures.withPanelSides(v)));
        settings.add(enumButton("car_indicator", FixtureConfig.CarIndicator.values(), fixtures.carIndicator(), v -> fixtures = fixtures.withCarIndicator(v)));
        settings.add(onOff("left_indicator", fixtures.leftPanelIndicator(), v -> fixtures = fixtures.withLeftPanelIndicator(v)));
        // 停止階の間隔が狭いと、出入口の上は上の階の床で隠れるので選べない
        boolean overDoor = panel.allowsOverDoorHallFixtures();
        if (!overDoor) {
            fixtures = fixtures.withoutOverDoorHallFixtures();
        }
        settings.add(enumButton("hall_indicator", overDoor ? FixtureConfig.HallIndicator.values()
                : new FixtureConfig.HallIndicator[] {FixtureConfig.HallIndicator.OVER_BUTTONS},
                fixtures.hallIndicator(), v -> fixtures = fixtures.withHallIndicator(v)));
        settings.add(enumButton("hall_lantern", overDoor ? FixtureConfig.HallLanternPlacement.values()
                : new FixtureConfig.HallLanternPlacement[] {FixtureConfig.HallLanternPlacement.NONE, FixtureConfig.HallLanternPlacement.BESIDE_DOOR},
                fixtures.hallLantern(), v -> fixtures = fixtures.withHallLantern(v)));
        settings.add(onOff("arrival_announcement", announcements.arrival(), v -> announcements = announcements.withArrival(v)));
        settings.add(onOff("close_announcement", announcements.doorClose(), v -> announcements = announcements.withDoorClose(v)));
        settings.add(onOff("chime", announcements.chime(), v -> announcements = announcements.withChime(v)));

        // 収まらない行は次の列へ折り返す。列の幅は、停止階の欄を残した幅を列の数で分ける
        int columns = (settings.size() + rowsFit - 1) / rowsFit;
        int settingsArea = width - MARGIN * 2 - FLOOR_COLUMN_WIDTH - MARGIN;
        int columnWidth = Math.max(60, Math.min(MAX_SETTING_WIDTH, (settingsArea - GAP * (columns - 1)) / columns));
        int settingsWidth = columns * columnWidth + GAP * (columns - 1);
        int left = Math.max(MARGIN, (width - settingsWidth - MARGIN - FLOOR_COLUMN_WIDTH) / 2);
        for (int i = 0; i < settings.size(); i++) {
            AbstractWidget widget = settings.get(i);
            widget.setX(left + (i / rowsFit) * (columnWidth + GAP));
            widget.setY(TOP + (i % rowsFit) * ROW);
            widget.setWidth(columnWidth);
            addRenderableWidget(widget);
        }

        // 停止階と階名（上の階から順に並べる）
        floorColumnX = left + settingsWidth + MARGIN;
        labelBoxes.clear();
        int shown = Math.min(visibleFloors, floors.length);
        scroll = Math.clamp(scroll, 0, Math.max(0, floors.length - visibleFloors));
        for (int row = 0; row < shown; row++) {
            int index = floors.length - 1 - (row + scroll);
            int floorY = floors[index];
            EditBox box = new EditBox(font, floorColumnX + 90, TOP + row * ROW, 50, 18, Component.translatable("gui.realelevator.panel.label"));
            box.setMaxLength(ControlPanelBlockEntity.MAX_LABEL_LENGTH);
            box.setHint(Component.literal(String.valueOf(index + 1)));
            box.setValue(panel.label(floorY));
            box.setResponder(text -> send());
            labelBoxes.add(addRenderableWidget(box));
        }
        if (floors.length > visibleFloors) {
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollBy(-1)).bounds(floorColumnX + 146, TOP, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollBy(1))
                    .bounds(floorColumnX + 146, TOP + (visibleFloors - 1) * ROW, 20, 20).build());
        }

        // 下の4つのボタンは、画面の幅に合わせて縮める
        int bottom = height - 26;
        int buttonWidth = Math.min(110, (width - MARGIN * 2 - GAP * 3) / 4);
        int buttonsLeft = (width - buttonWidth * 4 - GAP * 3) / 2;
        CycleButton<Boolean> previewButton = addRenderableWidget(CycleButton.onOffBuilder(preview)
                .create(buttonsLeft, bottom, buttonWidth, 20, Component.translatable("gui.realelevator.panel.preview"), (b, v) -> {
                    preview = v;
                    send();
                }));
        previewButton.active = !locked;
        Button install = addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.install"), b -> {
            PacketDistributor.sendToServer(new PanelPayloads.Install(menu.pos()));
            onClose();
        }).bounds(buttonsLeft + buttonWidth + GAP, bottom, buttonWidth, 20).build());
        install.active = !locked;
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.power"), b -> switchView(true))
                .bounds(buttonsLeft + (buttonWidth + GAP) * 2, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(buttonsLeft + (buttonWidth + GAP) * 3, bottom, buttonWidth, 20).build());
    }

    private void switchView(boolean power) {
        showPower = power;
        rebuildWidgets();
    }

    private void scrollBy(int amount) {
        scroll += amount;
        rebuildWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ControlPanelBlockEntity panel = panel();
        if (panel != null && panel.hasNetherStar() != hadStar) {
            hadStar = panel.hasNetherStar();
            // ネザースターを取り出すと、サーバーが高速仕様の設定を戻すので、それに合わせる
            speed = panel.speed();
            if (!showPower) {
                rebuildWidgets();
            }
        }
    }

    private interface Setter<T> {
        void set(T value);
    }

    private <T extends Enum<T>> CycleButton<T> enumButton(String key, T[] values, T initial, Setter<T> setter) {
        return CycleButton.<T>builder(v -> Component.translatable("gui.realelevator." + key + "." + v.name().toLowerCase(Locale.ROOT)))
                .withValues(values).withInitialValue(initial)
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel." + key), (b, v) -> {
                    setter.set(v);
                    send();
                });
    }

    private CycleButton<Boolean> onOff(String key, boolean initial, Setter<Boolean> setter) {
        return CycleButton.onOffBuilder(initial)
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel." + key), (b, v) -> {
                    setter.set(v);
                    send();
                });
    }

    /** min から max まで {@value SpeedSetting#STEP} 刻みの値を選ぶスライダー。動かし終えたときにサーバーへ送る。 */
    private class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final double min;
        private final double max;
        private final Setter<Double> setter;

        ValueSlider(String key, double initial, double min, double max, boolean active, Setter<Double> setter) {
            super(0, 0, MAX_SETTING_WIDTH, 20, Component.empty(), max > min ? (initial - min) / (max - min) : 0);
            this.key = key;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.active = active;
            updateMessage();
        }

        private double current() {
            double raw = min + (max - min) * value;
            return Math.round(raw / SpeedSetting.STEP) * SpeedSetting.STEP;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.realelevator.panel." + key, String.format(Locale.ROOT, "%.1f", current())));
        }

        @Override
        protected void applyValue() {
            setter.set(current());
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            send();
        }
    }

    /** 今の設定をサーバーへ送る。 */
    private void send() {
        ControlPanelBlockEntity panel = panel();
        if (panel == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Model", model.name());
        tag.putString("Speed", speed.spec().name());
        tag.putDouble("Acceleration", speed.acceleration());
        tag.putDouble("Jerk", speed.jerk());
        tag.putString("Pit", pitMode.name());
        tag.put("Fixtures", ConfigTags.writeFixtures(fixtures));
        tag.put("Announcements", ConfigTags.writeAnnouncements(announcements));
        tag.putBoolean("Preview", preview);
        // 画面に出ていない階の階名は、制御盤にあるものをそのまま送る
        CompoundTag labels = new CompoundTag();
        for (int floorY : floors) {
            labels.putString(Integer.toString(floorY), panel.label(floorY));
        }
        for (int row = 0; row < labelBoxes.size(); row++) {
            int floorY = floors[floors.length - 1 - (row + scroll)];
            labels.putString(Integer.toString(floorY), labelBoxes.get(row).getValue());
        }
        tag.put("Labels", labels);
        PacketDistributor.sendToServer(new PanelPayloads.Settings(menu.pos(), tag));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 階名の入力中は、持ち物のキー（E）などで画面を閉じない
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && getFocused() instanceof EditBox box && box.isFocused()) {
            box.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // コンテナの画面はドラッグをスロットにしか渡さないので、スライダーを動かせるよう先に渡す
        if (button == 0 && isDragging() && getFocused() instanceof ValueSlider slider
                && slider.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (!showPower) {
            return;
        }
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        graphics.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, PANEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL_FILL);
        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x, y + slot.y);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, PANEL_LIGHT);
        graphics.fill(x - 1, y - 1, x + 16, y + 16, SLOT_DARK);
        graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!showPower) {
            return;
        }
        graphics.drawString(font, Component.translatable("gui.realelevator.panel.star"), 8, 6, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ControlPanelBlockEntity panel = panel();
        if (panel == null) {
            return;
        }
        if (showPower) {
            renderPower(graphics, panel);
        } else {
            renderSettings(graphics, panel);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderPower(GuiGraphics graphics, ControlPanelBlockEntity panel) {
        graphics.drawCenteredString(font, Component.translatable("gui.realelevator.panel.power"), width / 2, Math.max(4, topPos - 34), 0xFFFFFF);
        Component note = panel.isStarLocked()
                ? Component.translatable("gui.realelevator.panel.star_locked")
                : Component.translatable("gui.realelevator.panel.star_hint");
        int noteWidth = Math.min(width - MARGIN * 2, 320);
        graphics.drawWordWrap(font, note, (width - noteWidth) / 2, Math.max(16, topPos - 22), noteWidth, 0xFFFF80);
    }

    private void renderSettings(GuiGraphics graphics, ControlPanelBlockEntity panel) {
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        int shown = Math.min(visibleFloors, floors.length);
        for (int row = 0; row < shown; row++) {
            int index = floors.length - 1 - (row + scroll);
            graphics.drawString(font, Component.translatable("gui.realelevator.panel.floor", index + 1, floors[index]),
                    floorColumnX, TOP + 5 + row * ROW, 0xE0E0E0);
        }
        if (floors.length <= 1) {
            graphics.drawWordWrap(font, Component.translatable("gui.realelevator.panel.no_floors"),
                    floorColumnX, TOP + 5 + ROW, FLOOR_COLUMN_WIDTH, 0xA0A0A0);
        } else if (!panel.allowsOverDoorHallFixtures() && shown < visibleFloors) {
            graphics.drawWordWrap(font, Component.translatable("gui.realelevator.panel.no_over_door"),
                    floorColumnX, TOP + 5 + shown * ROW, FLOOR_COLUMN_WIDTH, 0xA0A0A0);
        }
        graphics.drawCenteredString(font, status(panel), width / 2, height - 38, 0xFFFF80);
    }

    private Component status(ControlPanelBlockEntity panel) {
        if (panel.isInstalled()) {
            return Component.translatable("gui.realelevator.panel.status.installed");
        }
        FloorProblem problem = panel.floorProblem();
        if (problem != FloorProblem.NONE) {
            return Component.translatable("message.realelevator.install.floors." + problem.name().toLowerCase(Locale.ROOT));
        }
        if (frames++ % OBSTRUCTION_REFRESH == 0 && minecraft != null && minecraft.level != null) {
            obstructionCount = panel.obstructions(minecraft.level).size();
        }
        return obstructionCount > 0
                ? Component.translatable("message.realelevator.install.obstructed", obstructionCount)
                : Component.translatable("gui.realelevator.panel.status.ready");
    }
}
