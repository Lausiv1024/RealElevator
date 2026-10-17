package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import net.lausiv1024.realelevator.block.ElevatorPlan;
import net.lausiv1024.realelevator.core.car.FloorEntrances;
import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 設置用ブロックの設定画面と、保守用レンチのメニューで共通の部分。配置とアナウンスの設定、停止階と階名の欄、その並べ方。
 *
 * <p>画面の高さに収まる行数を求め、設定のボタンは足りない分だけ列を増やして折り返し、停止階は収まる行数だけを出してスクロールする。
 * 左に設定、右に停止階と階名を並べる。
 */
final class SettingsForm {
    static final int ROW = 22;
    static final int TOP = 28;
    static final int MARGIN = 10;
    static final int GAP = 4;
    /** 設定のボタンの幅の上限。 */
    static final int MAX_SETTING_WIDTH = 200;
    /** 停止階の欄の幅（「◯階（Y=◯）」、階名の入力欄、出入口のボタン、スクロールのボタン）。 */
    static final int FLOOR_COLUMN_WIDTH = 194;
    /** 出入口のボタンと、スクロールのボタンの、停止階の欄の左端からの位置。 */
    private static final int ENTRANCE_BUTTON_X = 144;
    private static final int SCROLL_BUTTON_X = 170;

    interface Setter<T> {
        void set(T value);
    }

    private FixtureConfig fixtures;
    private AnnouncementConfig announcements;
    private int[] floors;
    /** 停止階のY座標ごとの階名。入力欄の内容で更新する。 */
    private final Map<Integer, String> labels = new HashMap<>();
    /** 停止階のY座標ごとの出入口（二方向出入口のとき）。 */
    private final Map<Integer, FloorEntrances> entrances = new HashMap<>();
    /** 二方向出入口で、停止階ごとの出入口を選べるか（設置する前の設定画面だけ）。 */
    private boolean editableEntrances;
    private final List<EditBox> labelBoxes = new ArrayList<>();
    private int scroll;
    private int visibleFloors;
    private int floorColumnX;

    SettingsForm(CompoundTag settings) {
        this.fixtures = ConfigTags.readFixtures(settings.getCompound("Fixtures"));
        this.announcements = ConfigTags.readAnnouncements(settings.getCompound("Announcements"));
        setFloors(settings.getIntArray("Floors"));
        CompoundTag labelTag = settings.getCompound("Labels");
        CompoundTag entranceTag = settings.getCompound("Entrances");
        for (int floorY : floors) {
            labels.put(floorY, labelTag.getString(Integer.toString(floorY)));
            entrances.put(floorY, ConfigTags.parseEnum(FloorEntrances.class, entranceTag.getString(Integer.toString(floorY)), FloorEntrances.FRONT));
        }
    }

    /** 停止階ごとの出入口のボタンを出すか（二方向出入口で、設置する前）。 */
    void setEditableEntrances(boolean editable) {
        this.editableEntrances = editable;
    }

    /** 停止階を変える（設置用ブロックで登録し直されたとき）。 */
    void setFloors(int[] floors) {
        this.floors = floors.clone();
    }

    void setLabel(int floorY, String label) {
        labels.put(floorY, label);
    }

    boolean allowsOverDoorHallFixtures() {
        return ShaftLayout.allowsOverDoorHallFixtures(floors);
    }

    /** 配置とアナウンスの設定のボタン。値を変えると send を呼ぶ。 */
    List<AbstractWidget> fixtureWidgets(Runnable send) {
        List<AbstractWidget> widgets = new ArrayList<>();
        widgets.add(enumButton("panel_sides", FixtureConfig.PanelSides.values(), fixtures.panelSides(), v -> fixtures = fixtures.withPanelSides(v), send));
        widgets.add(enumButton("car_indicator", FixtureConfig.CarIndicator.values(), fixtures.carIndicator(), v -> fixtures = fixtures.withCarIndicator(v), send));
        widgets.add(onOff("left_indicator", fixtures.leftPanelIndicator(), v -> fixtures = fixtures.withLeftPanelIndicator(v), send));
        // 停止階の間隔が狭いと、出入口の上は上の階の床で隠れるので選べない
        boolean overDoor = allowsOverDoorHallFixtures();
        if (!overDoor) {
            fixtures = fixtures.withoutOverDoorHallFixtures();
        }
        widgets.add(enumButton("hall_indicator", overDoor ? FixtureConfig.HallIndicator.values()
                : new FixtureConfig.HallIndicator[] {FixtureConfig.HallIndicator.OVER_BUTTONS},
                fixtures.hallIndicator(), v -> fixtures = fixtures.withHallIndicator(v), send));
        widgets.add(enumButton("hall_lantern", overDoor ? FixtureConfig.HallLanternPlacement.values()
                : new FixtureConfig.HallLanternPlacement[] {FixtureConfig.HallLanternPlacement.NONE, FixtureConfig.HallLanternPlacement.BESIDE_DOOR},
                fixtures.hallLantern(), v -> fixtures = fixtures.withHallLantern(v), send));
        widgets.add(onOff("arrival_announcement", announcements.arrival(), v -> announcements = announcements.withArrival(v), send));
        widgets.add(onOff("close_announcement", announcements.doorClose(), v -> announcements = announcements.withDoorClose(v), send));
        widgets.add(onOff("chime", announcements.chime(), v -> announcements = announcements.withChime(v), send));
        return widgets;
    }

    /**
     * 設定のボタンと停止階の欄を並べる。
     *
     * @param bottom   使える範囲の下端（下のボタンと状態の文の上）
     * @param add      画面に加える
     * @param send     階名を変えたときに呼ぶ
     * @param rebuild  スクロールしたときに画面を作り直す
     */
    void layout(Font font, int width, int bottom, List<AbstractWidget> settings, Consumer<AbstractWidget> add,
                Runnable send, Runnable rebuild) {
        int rowsFit = Math.max(1, (bottom - 4 - TOP) / ROW);
        visibleFloors = rowsFit;

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
            add.accept(widget);
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
            box.setMaxLength(ElevatorPlan.MAX_LABEL_LENGTH);
            box.setHint(Component.literal(String.valueOf(index + 1)));
            box.setValue(labels.getOrDefault(floorY, ""));
            box.setResponder(text -> {
                labels.put(floorY, text);
                send.run();
            });
            labelBoxes.add(box);
            add.accept(box);
            if (editableEntrances) {
                // 最下階（設置用ブロックの階）は前を必ず使う
                boolean lowest = index == 0;
                List<FloorEntrances> values = new ArrayList<>();
                for (FloorEntrances value : FloorEntrances.values()) {
                    if (!lowest || value.allowedOnLowestFloor()) {
                        values.add(value);
                    }
                }
                FloorEntrances initial = entrances.getOrDefault(floorY, FloorEntrances.FRONT);
                if (!values.contains(initial)) {
                    initial = FloorEntrances.FRONT;
                }
                add.accept(CycleButton.<FloorEntrances>builder(v -> Component.translatable(
                                "gui.realelevator.entrance." + v.name().toLowerCase(Locale.ROOT)))
                        .withValues(values).withInitialValue(initial).displayOnlyValue()
                        .create(floorColumnX + ENTRANCE_BUTTON_X, TOP + row * ROW, 24, 18,
                                Component.translatable("gui.realelevator.panel.entrances"), (b, v) -> {
                                    entrances.put(floorY, v);
                                    send.run();
                                }));
            }
        }
        if (floors.length > visibleFloors) {
            add.accept(Button.builder(Component.literal("▲"), b -> {
                scroll--;
                rebuild.run();
            }).bounds(floorColumnX + SCROLL_BUTTON_X, TOP, 20, 20).build());
            add.accept(Button.builder(Component.literal("▼"), b -> {
                scroll++;
                rebuild.run();
            }).bounds(floorColumnX + SCROLL_BUTTON_X, TOP + (visibleFloors - 1) * ROW, 20, 20).build());
        }
    }

    /** 停止階の名前と、停止階についての注意を描く。 */
    void renderFloors(GuiGraphics graphics, Font font) {
        int shown = Math.min(visibleFloors, floors.length);
        for (int row = 0; row < shown; row++) {
            int index = floors.length - 1 - (row + scroll);
            graphics.drawString(font, Component.translatable("gui.realelevator.panel.floor", index + 1, floors[index]),
                    floorColumnX, TOP + 5 + row * ROW, 0xE0E0E0);
        }
        if (floors.length <= 1) {
            graphics.drawWordWrap(font, Component.translatable("gui.realelevator.panel.no_floors"),
                    floorColumnX, TOP + 5 + ROW, FLOOR_COLUMN_WIDTH, 0xA0A0A0);
        } else if (!allowsOverDoorHallFixtures() && shown < visibleFloors) {
            graphics.drawWordWrap(font, Component.translatable("gui.realelevator.panel.no_over_door"),
                    floorColumnX, TOP + 5 + shown * ROW, FLOOR_COLUMN_WIDTH, 0xA0A0A0);
        }
    }

    /** 配置・アナウンス・階名を tag に書く（{@code ElevatorPlan#applyRenewal} の形）。 */
    void writeTo(CompoundTag tag) {
        tag.put("Fixtures", ConfigTags.writeFixtures(fixtures));
        tag.put("Announcements", ConfigTags.writeAnnouncements(announcements));
        CompoundTag labelTag = new CompoundTag();
        for (int floorY : floors) {
            labelTag.putString(Integer.toString(floorY), labels.getOrDefault(floorY, ""));
        }
        tag.put("Labels", labelTag);
        CompoundTag entranceTag = new CompoundTag();
        for (int floorY : floors) {
            entranceTag.putString(Integer.toString(floorY), entrances.getOrDefault(floorY, FloorEntrances.FRONT).name());
        }
        tag.put("Entrances", entranceTag);
    }

    static <T extends Enum<T>> CycleButton<T> enumButton(String key, T[] values, T initial, Setter<T> setter, Runnable send) {
        return CycleButton.<T>builder(v -> Component.translatable("gui.realelevator." + key + "." + v.name().toLowerCase(Locale.ROOT)))
                .withValues(values).withInitialValue(initial)
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel." + key), (b, v) -> {
                    setter.set(v);
                    send.run();
                });
    }

    static CycleButton<Boolean> onOff(String key, boolean initial, Setter<Boolean> setter, Runnable send) {
        return CycleButton.onOffBuilder(initial)
                .create(0, 0, MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel." + key), (b, v) -> {
                    setter.set(v);
                    send.run();
                });
    }
}
