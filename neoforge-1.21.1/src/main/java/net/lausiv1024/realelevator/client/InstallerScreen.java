package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import net.lausiv1024.realelevator.block.ElevatorPlan;
import net.lausiv1024.realelevator.block.InstallerBlockEntity;
import net.lausiv1024.realelevator.block.MaterialStacks;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.install.InstallMaterials;
import net.lausiv1024.realelevator.core.install.ShaftLayout.FloorProblem;
import net.lausiv1024.realelevator.core.install.ShaftLayout.CounterweightPosition;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.lausiv1024.realelevator.menu.InstallerMenu;
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
 * 設置用ブロックの設定画面。「設定」と「動力」の2つの表示を、下のボタンで切り替える。
 *
 * <ul>
 * <li>設定：左に機種・速度・ピット・配置・アナウンスの設定、右に停止階と階名、下にプレビューと確定を並べる（{@link SettingsForm}）。</li>
 * <li>動力：ネザースターのスロットと持ち物。ネザースターをはめると、高速仕様を選べるようになる。</li>
 * </ul>
 *
 * 設定を変えるたびにサーバーへ送り、サーバーの設置用ブロックが反映して送り返す。
 */
public class InstallerScreen extends AbstractContainerScreen<InstallerMenu> {
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
    private DoorType door;
    private SpeedSetting speed;
    private PitMode pitMode;
    private CounterweightPosition counterweight;
    private boolean through;
    private boolean observation;
    private boolean preview;
    @Nullable
    private SettingsForm form;
    /** 「動力」の表示か。 */
    private boolean showPower;
    /** 前に見たときにネザースターがはまっていたか。変わったら、選べる速度が変わるので作り直す。 */
    private boolean hadStar;
    private int obstructionCount;
    /** プロファイルを読み込んだ結果。次に設定を変えるまで、状態の欄に出す。 */
    @Nullable
    private Component notice;
    private int frames;

    public InstallerScreen(InstallerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = InstallerMenu.INVENTORY_Y - 11;
    }

    @Nullable
    private InstallerBlockEntity installer() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.pos()) instanceof InstallerBlockEntity installer ? installer : null;
    }

    @Override
    protected void init() {
        super.init();
        InstallerBlockEntity installer = installer();
        if (installer == null) {
            onClose();
            return;
        }
        if (form == null) {
            model = installer.model();
            door = installer.plan().spec().door();
            speed = installer.speed();
            pitMode = installer.pitMode();
            counterweight = installer.plan().counterweight();
            through = installer.plan().isThrough();
            observation = installer.plan().isObservation();
            preview = installer.isPreviewing();
            hadStar = installer.hasNetherStar();
            form = new SettingsForm(installer.settingsTag());
            // プリセットの「展望」を選んだら展望仕様にする（設置する前だけ）
            form.setObservationHandler(v -> {
                observation = v;
                through &= !v;
            });
        }
        form.setFloors(installer.floorYs());
        menu.setSlotsVisible(showPower);
        if (showPower) {
            initPower();
        } else {
            initSettings(installer);
        }
    }

    private void initPower() {
        int bottom = topPos + imageHeight + 6;
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.back"), b -> switchView(false))
                .bounds(leftPos, bottom, 86, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(leftPos + imageWidth - 86, bottom, 86, 20).build());
    }

    private void initSettings(InstallerBlockEntity installer) {
        List<AbstractWidget> settings = new ArrayList<>();
        settings.add(CycleButton.<CarModel>builder(m -> Component.literal(m.name().replace("P", "P-")))
                .withValues(CarModel.values()).withInitialValue(model)
                .create(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.model"), (b, v) -> {
                    model = v;
                    // 機種で選べない扉の方式は CO に戻す
                    door = new CarSpec(model, door).door();
                    send();
                    rebuildWidgets();
                }));
        List<DoorType> doors = new ArrayList<>();
        for (DoorType type : DoorType.values()) {
            if (type.isAvailable(model)) {
                doors.add(type);
            }
        }
        settings.add(CycleButton.<DoorType>builder(d -> Component.translatable("gui.realelevator.door." + d.name().toLowerCase(Locale.ROOT)))
                .withValues(doors).withInitialValue(door)
                .create(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.door"), (b, v) -> {
                    door = v;
                    send();
                }));
        // 高速仕様は、ネザースターがはまっているときだけ出す
        List<SpeedSpec> speeds = new ArrayList<>();
        for (SpeedSpec spec : SpeedSpec.values()) {
            if (!spec.isHighSpeed() || installer.hasNetherStar() || spec == speed.spec()) {
                speeds.add(spec);
            }
        }
        settings.add(CycleButton.<SpeedSpec>builder(sp -> Component.translatable(
                        "gui.realelevator.speed." + sp.name().toLowerCase(Locale.ROOT)))
                .withValues(speeds).withInitialValue(speed.spec())
                .create(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.speed"), (b, v) -> {
                    speed = speed.withSpec(v);
                    send();
                    // 加速度とジャークを動かせるかが変わる
                    rebuildWidgets();
                }));
        settings.add(new ValueSlider("acceleration", speed.acceleration(), speed.minAcceleration(), SpeedSetting.MAX_ACCELERATION,
                speed.isAdjustable(), v -> speed = speed.withAcceleration(v)));
        settings.add(new ValueSlider("jerk", speed.jerk(), speed.minJerk(), SpeedSetting.MAX_JERK,
                speed.isAdjustable(), v -> speed = speed.withJerk(v)));
        settings.add(CycleButton.<PitMode>builder(p -> Component.translatable(
                        "gui.realelevator.pit." + p.name().toLowerCase(Locale.ROOT)))
                .withValues(PitMode.values()).withInitialValue(pitMode)
                .create(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.pit"), (b, v) -> {
                    pitMode = v;
                    send();
                }));
        settings.add(CycleButton.onOffBuilder(through)
                .create(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.through"), (b, v) -> {
                    through = v;
                    // 展望仕様とは組み合わせない
                    observation &= !v;
                    send();
                    // 停止階の出入口のボタンと、釣合い重りの選べる値が変わる
                    rebuildWidgets();
                }));
        settings.add(CycleButton.onOffBuilder(observation)
                .create(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.observation"), (b, v) -> {
                    observation = v;
                    through &= !v;
                    send();
                    rebuildWidgets();
                }));
        // 二方向出入口と展望仕様では横落としに決まる
        boolean sideOnly = through || observation;
        CycleButton<CounterweightPosition> counterweightButton = CycleButton.<CounterweightPosition>builder(c -> Component.translatable(
                        "gui.realelevator.counterweight." + c.name().toLowerCase(Locale.ROOT)))
                .withValues(CounterweightPosition.values()).withInitialValue(sideOnly ? CounterweightPosition.SIDE : counterweight)
                .create(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.translatable("gui.realelevator.panel.counterweight"), (b, v) -> {
                    counterweight = v;
                    send();
                });
        counterweightButton.active = !sideOnly;
        settings.add(counterweightButton);
        settings.addAll(form.fixtureWidgets(this::send, this::rebuildWidgets));
        form.setEditableEntrances(through);
        form.layout(font, width, height - 42, settings, this::addRenderableWidget, this::send, this::rebuildWidgets);

        // 下の5つのボタンは、画面の幅に合わせて縮める
        int bottom = height - 26;
        int gap = SettingsForm.GAP;
        int buttonWidth = Math.min(100, (width - SettingsForm.MARGIN * 2 - gap * 4) / 5);
        int buttonsLeft = (width - buttonWidth * 5 - gap * 4) / 2;
        addRenderableWidget(CycleButton.onOffBuilder(preview)
                .create(buttonsLeft, bottom, buttonWidth, 20, Component.translatable("gui.realelevator.panel.preview"), (b, v) -> {
                    preview = v;
                    send();
                }));
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.install"), b -> {
            PacketDistributor.sendToServer(new PanelPayloads.Install(menu.pos()));
            onClose();
        }).bounds(buttonsLeft + buttonWidth + gap, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.profile"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(ProfileScreen.forSettings(this, settingsTag(), this::applyProfile, false));
            }
        }).bounds(buttonsLeft + (buttonWidth + gap) * 2, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.panel.power"), b -> switchView(true))
                .bounds(buttonsLeft + (buttonWidth + gap) * 3, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(buttonsLeft + (buttonWidth + gap) * 4, bottom, buttonWidth, 20).build());
    }

    private void switchView(boolean power) {
        showPower = power;
        rebuildWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        InstallerBlockEntity installer = installer();
        if (installer != null && installer.hasNetherStar() != hadStar) {
            hadStar = installer.hasNetherStar();
            // ネザースターを取り出すと、サーバーが高速仕様の設定を戻すので、それに合わせる
            speed = installer.speed();
            if (!showPower) {
                rebuildWidgets();
            }
        }
    }

    /** min から max まで {@value SpeedSetting#STEP} 刻みの値を選ぶスライダー。動かし終えたときにサーバーへ送る。 */
    private class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final double min;
        private final double max;
        private final SettingsForm.Setter<Double> setter;

        ValueSlider(String key, double initial, double min, double max, boolean active, SettingsForm.Setter<Double> setter) {
            super(0, 0, SettingsForm.MAX_SETTING_WIDTH, 20, Component.empty(), max > min ? (initial - min) / (max - min) : 0);
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

    /** 今の設定。{@link InstallerBlockEntity#applySettings} と同じ形。 */
    private CompoundTag settingsTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Model", model.name());
        tag.putString("Door", door.name());
        tag.putString("Speed", speed.spec().name());
        tag.putDouble("Acceleration", speed.acceleration());
        tag.putDouble("Jerk", speed.jerk());
        tag.putString("Pit", pitMode.name());
        tag.putString("Counterweight", counterweight.name());
        tag.putBoolean("Through", through);
        tag.putBoolean("Observation", observation);
        tag.putBoolean("Preview", preview);
        if (form != null) {
            form.writeTo(tag);
        }
        return tag;
    }

    /** 今の設定をサーバーへ送る。 */
    private void send() {
        if (form == null) {
            return;
        }
        notice = null;
        PacketDistributor.sendToServer(new PanelPayloads.Settings(menu.pos(), settingsTag()));
    }

    /**
     * プロファイル profile を読み込む（停止階・階名・読み上げ名・出入口はそのまま）。高速仕様は、ネザースターがはまっていなければ
     * 速度を変えずに、その旨を出す。
     */
    private void applyProfile(CompoundTag profile) {
        InstallerBlockEntity installer = installer();
        if (installer == null || form == null) {
            return;
        }
        model = ConfigTags.parseEnum(CarModel.class, profile.getString("Model"), model);
        door = new CarSpec(model, ConfigTags.parseEnum(DoorType.class, profile.getString("Door"), door)).door();
        SpeedSpec requested = ConfigTags.parseEnum(SpeedSpec.class, profile.getString("Speed"), speed.spec());
        boolean speedRefused = requested.isHighSpeed() && !installer.hasNetherStar();
        if (!speedRefused) {
            speed = ElevatorPlan.readSpeed(requested, profile);
        }
        pitMode = ConfigTags.parseEnum(PitMode.class, profile.getString("Pit"), pitMode);
        counterweight = ConfigTags.parseEnum(CounterweightPosition.class, profile.getString("Counterweight"), counterweight);
        through = profile.getBoolean("Through");
        observation = profile.getBoolean("Observation") && !through;
        form.applyProfile(profile);
        send();
        notice = Component.translatable(speedRefused ? "message.realelevator.profile.applied_no_star" : "message.realelevator.profile.applied");
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
        InstallerBlockEntity installer = installer();
        if (installer == null || form == null) {
            return;
        }
        if (showPower) {
            graphics.drawCenteredString(font, Component.translatable("gui.realelevator.panel.power"), width / 2, Math.max(4, topPos - 34), 0xFFFFFF);
            int noteWidth = Math.min(width - SettingsForm.MARGIN * 2, 320);
            graphics.drawWordWrap(font, Component.translatable("gui.realelevator.panel.star_hint"),
                    (width - noteWidth) / 2, Math.max(16, topPos - 22), noteWidth, 0xFFFF80);
        } else {
            graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
            form.renderFloors(graphics, font);
            graphics.drawCenteredString(font, status(installer), width / 2, height - 38, 0xFFFF80);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component status(InstallerBlockEntity installer) {
        if (notice != null) {
            return notice;
        }
        FloorProblem problem = installer.floorProblem();
        if (problem != FloorProblem.NONE) {
            return Component.translatable("message.realelevator.install.floors." + problem.name().toLowerCase(Locale.ROOT));
        }
        if (frames++ % OBSTRUCTION_REFRESH == 0 && minecraft != null && minecraft.level != null) {
            obstructionCount = installer.obstructions(minecraft.level).size();
        }
        if (obstructionCount > 0) {
            return Component.translatable("message.realelevator.install.obstructed", obstructionCount);
        }
        // クリエイティブでは材料を使わない
        if (minecraft == null || minecraft.player == null || minecraft.player.getAbilities().instabuild) {
            return Component.translatable("gui.realelevator.panel.status.ready");
        }
        InstallMaterials materials = installer.requiredMaterials();
        return MaterialStacks.has(minecraft.player, materials)
                ? Component.translatable("gui.realelevator.panel.status.ready_materials", MaterialStacks.describe(materials))
                : Component.translatable("message.realelevator.install.no_materials", MaterialStacks.describe(materials));
    }
}
