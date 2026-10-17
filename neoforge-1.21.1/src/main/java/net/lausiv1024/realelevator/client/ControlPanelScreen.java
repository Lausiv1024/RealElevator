package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.block.ControlPanelBlockEntity;
import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.install.ShaftLayout.FloorProblem;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.lausiv1024.realelevator.network.PanelPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 制御盤の設定画面。左に機種・速度・ピット・配置・アナウンスの設定、右に停止階と階名、下にプレビューと確定を並べる。
 * 設定を変えるたびにサーバーへ送り、サーバーの制御盤が反映して送り返す。
 *
 * <p>画面の高さに収まる行数を求め、設定のボタンは足りない分だけ列を増やして折り返し、停止階は収まる行数だけを出してスクロールする。
 */
public class ControlPanelScreen extends Screen {
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

    private final BlockPos pos;
    private CarModel model;
    private SpeedSpec speed;
    private PitMode pitMode;
    private FixtureConfig fixtures;
    private AnnouncementConfig announcements;
    private boolean preview;
    private final List<EditBox> labelBoxes = new ArrayList<>();
    private int[] floors = new int[0];
    private int scroll;
    /** 停止階を一度に出す行数。画面の高さで決まる。 */
    private int visibleFloors;
    /** 停止階の欄の左端。 */
    private int floorColumnX;
    private int obstructionCount;
    private int frames;

    private ControlPanelScreen(BlockPos pos) {
        super(Component.translatable("gui.realelevator.panel.title"));
        this.pos = pos;
    }

    /** 制御盤 pos の設定画面を開く。 */
    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ControlPanelScreen(pos));
    }

    @Nullable
    private ControlPanelBlockEntity panel() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof ControlPanelBlockEntity panel ? panel : null;
    }

    @Override
    protected void init() {
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
        }
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
        CycleButton<SpeedSpec> speedButton = CycleButton.<SpeedSpec>builder(sp -> Component.translatable(
                        "gui.realelevator.speed." + sp.name().toLowerCase(Locale.ROOT)))
                .withValues(SpeedSpec.NORMAL_LOW, SpeedSpec.NORMAL_HIGH).withInitialValue(speed.isHighSpeed() ? SpeedSpec.NORMAL_LOW : speed)
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.speed"), (b, v) -> {
                    speed = v;
                    send();
                });
        speedButton.active = !locked;
        settings.add(speedButton);
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

        // 下の3つのボタンは、画面の幅に合わせて縮める
        int bottom = height - 26;
        int buttonWidth = Math.min(130, (width - MARGIN * 2 - GAP * 2) / 3);
        int buttonsLeft = (width - buttonWidth * 3 - GAP * 2) / 2;
        CycleButton<Boolean> previewButton = addRenderableWidget(CycleButton.onOffBuilder(preview)
                .create(buttonsLeft, bottom, buttonWidth, 20, Component.translatable("gui.realelevator.panel.preview"), (b, v) -> {
                    preview = v;
                    send();
                }));
        previewButton.active = !locked;
        Button install = addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.install"), b -> {
            PacketDistributor.sendToServer(new PanelPayloads.Install(pos));
            onClose();
        }).bounds(buttonsLeft + buttonWidth + GAP, bottom, buttonWidth, 20).build());
        install.active = !locked;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(buttonsLeft + (buttonWidth + GAP) * 2, bottom, buttonWidth, 20).build());
    }

    private void scrollBy(int amount) {
        scroll += amount;
        rebuildWidgets();
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

    /** 今の設定をサーバーへ送る。 */
    private void send() {
        ControlPanelBlockEntity panel = panel();
        if (panel == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Model", model.name());
        tag.putString("Speed", speed.name());
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
        PacketDistributor.sendToServer(new PanelPayloads.Settings(pos, tag));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        ControlPanelBlockEntity panel = panel();
        if (panel == null) {
            return;
        }
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
