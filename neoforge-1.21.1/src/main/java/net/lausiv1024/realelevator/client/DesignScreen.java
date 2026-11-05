package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.design.DesignPreset;
import net.lausiv1024.realelevator.design.Materials;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * デザインの画面。設置用ブロックの設定画面と保守用レンチのリニューアル画面から開く。
 *
 * <p>左の列にプリセットと、天井・手すり・防犯窓・乗場の意匠の設定。中の列に素材を選ぶ部位。右に、選んだ部位の候補のブロックを
 * アイテムのアイコンで並べ、クリックで選ぶ（多ければホイールでスクロール）。トランサムの「前壁と同じ／ドアと同じ」、乗場の壁の
 * 「まわりに合わせる」、カーペットの「なし」は、候補の上のボタンで選ぶ。候補の下に、かご内と乗場を切り替えて見られるプレビューを出す。サーバーで自由設定がオンなら、手に持っているブロックも使える。
 * 値を変えるたびに設定を送る。
 */
final class DesignScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int TOP = 28;
    private static final int ROW = 22;
    private static final int LEFT_WIDTH = 150;
    private static final int PART_WIDTH = 100;
    private static final int GAP = 6;
    private static final int SLOT = 18;
    private static final int SELECTED = 0xFFFFD040;
    private static final int SLOT_BORDER = 0xFF555555;
    private static final int SLOT_FILL = 0xFF202020;
    /** プレビューに残す高さの下限 [px]。候補が多くても、これだけはプレビューに残す。 */
    private static final int MIN_PREVIEW_HEIGHT = 80;
    /** プレビューの幅の、高さに対する上限（広い画面で横長になりすぎないように）。 */
    private static final double MAX_PREVIEW_ASPECT = 1.8;
    private ElevatorPreview.View previewView = ElevatorPreview.View.CAR;

    /** 素材を選ぶ部位（カーペットは素材ではなく色）。 */
    private enum Choice {
        WALL(InteriorConfig.Part.WALL),
        FRONT(InteriorConfig.Part.FRONT),
        TRANSOM(InteriorConfig.Part.TRANSOM),
        DOOR(InteriorConfig.Part.DOOR),
        FLOOR(InteriorConfig.Part.FLOOR),
        CARPET(null),
        BASEBOARD(InteriorConfig.Part.BASEBOARD),
        LANDING_FRAME(InteriorConfig.Part.LANDING_FRAME),
        LANDING_WALL(InteriorConfig.Part.LANDING_WALL);

        @Nullable
        final InteriorConfig.Part part;

        Choice(@Nullable InteriorConfig.Part part) {
            this.part = part;
        }
    }

    private final Screen parent;
    private final SettingsForm form;
    private final Runnable send;
    private Choice choice = Choice.WALL;
    private DesignPreset preset;
    private int scroll;
    @Nullable
    private Component message;

    DesignScreen(Screen parent, SettingsForm form, Runnable send) {
        super(Component.translatable("gui.realelevator.design.title"));
        this.parent = parent;
        this.form = form;
        this.send = send;
        List<DesignPreset> presets = ClientPresets.presets();
        this.preset = presets.isEmpty() ? null : presets.get(0);
    }

    @Override
    protected void init() {
        int y = TOP;
        List<DesignPreset> presets = ClientPresets.presets();
        if (!presets.isEmpty()) {
            if (!presets.contains(preset)) {
                preset = presets.get(0);
            }
            addRenderableWidget(CycleButton.<DesignPreset>builder(p -> Component.translatable(p.name()))
                    .withValues(presets).withInitialValue(preset)
                    .create(MARGIN, y, LEFT_WIDTH - 44, 20, Component.translatable("gui.realelevator.design.preset"), (b, v) -> preset = v));
            addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.design.apply"), b -> applyPreset())
                    .bounds(MARGIN + LEFT_WIDTH - 40, y, 40, 20).build());
            y += ROW;
        }
        InteriorConfig interior = form.interior();
        addRenderableWidget(left(y, SettingsForm.enumButton("ceiling", InteriorConfig.CeilingType.values(), interior.ceiling(),
                v -> form.setInterior(form.interior().withCeiling(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("handrail", InteriorConfig.HandrailPlacement.values(), interior.handrail(),
                v -> form.setInterior(form.interior().withHandrail(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("security_window", InteriorConfig.SecurityWindow.values(), interior.securityWindow(),
                v -> form.setInterior(form.interior().withSecurityWindow(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("landing_frame", InteriorConfig.FrameStyle.values(), interior.landing().frame(),
                v -> form.setInterior(form.interior().withLanding(form.interior().landing().withFrame(v))), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("landing_panel", InteriorConfig.PanelStyle.values(), interior.landing().panel(),
                v -> form.setInterior(form.interior().withLanding(form.interior().landing().withPanel(v))), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.onOff("lantern_plate", interior.landing().lanternPlate(),
                v -> form.setInterior(form.interior().withLanding(form.interior().landing().withLanternPlate(v))), send)));

        // 部位（選んでいる部位のボタンは押せない見た目にする）
        int partX = MARGIN + LEFT_WIDTH + GAP;
        int partRow = Math.min(ROW, Math.max(16, (height - 34 - TOP) / Choice.values().length));
        for (Choice c : Choice.values()) {
            Button button = Button.builder(Component.translatable("gui.realelevator.design.part." + c.name().toLowerCase(Locale.ROOT)), b -> {
                choice = c;
                scroll = 0;
                message = null;
                rebuildWidgets();
            }).bounds(partX, TOP + c.ordinal() * partRow, PART_WIDTH, Math.min(20, partRow - 2)).build();
            button.active = c != choice;
            addRenderableWidget(button);
        }

        // 候補の上の、特別な値のボタン
        int gridX = gridLeft();
        int specialX = gridX;
        for (String value : specialValues()) {
            Component label = Component.translatable("gui.realelevator.design.value." + specialKey(value));
            int w = font.width(label) + 10;
            Button button = Button.builder(label, b -> select(value)).bounds(specialX, TOP, w, 20).build();
            button.active = !value.equals(current());
            addRenderableWidget(button);
            specialX += w + 4;
        }
        if (ClientPresets.freeMaterials() && choice.part != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.design.use_held"), b -> useHeldBlock())
                    .bounds(gridX, height - 52, Math.min(200, width - MARGIN - gridX), 20).build());
        }
        if (form.preview() != null) {
            int tabWidth = Math.min(100, (width - MARGIN - gridX - GAP) / 2);
            for (ElevatorPreview.View view : ElevatorPreview.View.values()) {
                Button button = Button.builder(Component.translatable("gui.realelevator.preview.tab." + view.name().toLowerCase(Locale.ROOT)), b -> {
                    previewView = view;
                    rebuildWidgets();
                }).bounds(previewLeft() + view.ordinal() * (tabWidth + GAP), previewTop() - ROW, tabWidth, 20).build();
                button.active = view != previewView;
                addRenderableWidget(button);
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 75, height - 26, 150, 20).build());
    }

    /** 候補の欄の下端（候補とプレビューを合わせた範囲）。 */
    private int rightBottom() {
        return ClientPresets.freeMaterials() && choice.part != null ? height - 56 : height - 32;
    }

    private int previewHeight() {
        return form.preview() == null ? 0 : rightBottom() - previewTop();
    }

    /**
     * プレビューの上端。切り替えのボタンはその上に置く。素材の候補には、どの部位の候補も収まる段数を先に取り（2段より少なくはしない）、
     * 残りの高さをプレビューにする。候補が多くても、プレビューには {@value #MIN_PREVIEW_HEIGHT}px は残す。
     */
    private int previewTop() {
        int area = rightBottom() - gridTop();
        int gridHeight = Math.min(candidateRowsNeeded() * SLOT, Math.max(SLOT * 2, area - ROW - 4 - MIN_PREVIEW_HEIGHT));
        return gridTop() + gridHeight + 4 + ROW;
    }

    /** どの部位の候補も収まる段数。 */
    private int candidateRowsNeeded() {
        int most = InteriorConfig.Carpet.values().length;
        for (Choice c : Choice.values()) {
            if (c.part != null) {
                most = Math.max(most, Materials.candidates(c.part).size() + 1);
            }
        }
        return (most + gridColumns() - 1) / gridColumns();
    }

    /** プレビューの幅。広い画面では、高さの {@value #MAX_PREVIEW_ASPECT} 倍までにして、候補の欄の中央に置く。 */
    private int previewWidth() {
        return Math.min(width - MARGIN - gridLeft(), (int) (previewHeight() * MAX_PREVIEW_ASPECT));
    }

    private int previewLeft() {
        return gridLeft() + (width - MARGIN - gridLeft() - previewWidth()) / 2;
    }

    private static CycleButton<?> left(int y, CycleButton<?> button) {
        button.setX(MARGIN);
        button.setY(y);
        button.setWidth(LEFT_WIDTH);
        return button;
    }

    private int gridLeft() {
        return MARGIN + LEFT_WIDTH + GAP + PART_WIDTH + GAP;
    }

    private int gridTop() {
        return specialValues().isEmpty() ? TOP : TOP + ROW;
    }

    private int gridColumns() {
        return Math.max(1, (width - MARGIN - gridLeft()) / SLOT);
    }

    private int gridRows() {
        int bottom = form.preview() != null ? previewTop() - ROW - 4 : rightBottom();
        return Math.max(1, (bottom - gridTop()) / SLOT);
    }

    /** 候補の上にボタンで出す、特別な値。 */
    private List<String> specialValues() {
        return switch (choice) {
            case TRANSOM -> List.of(InteriorConfig.SAME_AS_FRONT, InteriorConfig.SAME_AS_DOOR);
            case LANDING_WALL -> List.of(InteriorConfig.AUTO);
            case CARPET -> List.of(InteriorConfig.Carpet.NONE.name());
            default -> List.of();
        };
    }

    private static String specialKey(String value) {
        return switch (value) {
            case InteriorConfig.SAME_AS_FRONT -> "same_as_front";
            case InteriorConfig.SAME_AS_DOOR -> "same_as_door";
            case InteriorConfig.AUTO -> "auto";
            default -> "none";
        };
    }

    /** 選んでいる部位の、今の値（素材のID。カーペットは色の名前）。 */
    private String current() {
        InteriorConfig interior = form.interior();
        return choice.part != null ? interior.material(choice.part) : interior.carpet().name();
    }

    /** 候補のブロックのID（カーペットは色の名前と、そのカーペットのブロックのID）。 */
    private List<String> candidates() {
        List<String> values = new ArrayList<>();
        if (choice.part == null) {
            for (InteriorConfig.Carpet carpet : InteriorConfig.Carpet.values()) {
                if (carpet != InteriorConfig.Carpet.NONE) {
                    values.add(carpet.name());
                }
            }
            return values;
        }
        values.addAll(Materials.candidates(choice.part));
        // 自由設定で選んだ、タグにないブロックも出す
        String current = current();
        if (!values.contains(current) && !specialValues().contains(current)) {
            values.add(0, current);
        }
        return values;
    }

    private static Block blockOf(String value) {
        InteriorConfig.Carpet carpet = carpetOf(value);
        return Materials.block(carpet != null ? carpet.blockId() : value);
    }

    @Nullable
    private static InteriorConfig.Carpet carpetOf(String value) {
        for (InteriorConfig.Carpet carpet : InteriorConfig.Carpet.values()) {
            if (carpet.name().equals(value)) {
                return carpet;
            }
        }
        return null;
    }

    private void select(String value) {
        InteriorConfig interior = form.interior();
        if (choice.part == null) {
            InteriorConfig.Carpet carpet = carpetOf(value);
            form.setInterior(interior.withCarpet(carpet != null ? carpet : InteriorConfig.Carpet.NONE));
        } else {
            form.setInterior(interior.withMaterial(choice.part, value));
        }
        message = null;
        send.run();
        rebuildWidgets();
    }

    private void applyPreset() {
        if (preset == null) {
            return;
        }
        form.setInterior(preset.interior());
        if (preset.carIndicator() != null) {
            form.setFixtures(form.fixtures().withCarIndicator(preset.carIndicator()));
        }
        if (form.observationHandler() != null) {
            form.observationHandler().accept(preset.observation());
            message = null;
        } else {
            // リニューアルでは、展望仕様（奥壁のガラス）は変えられない
            message = preset.observation() ? Component.translatable("gui.realelevator.design.observation_install_only") : null;
        }
        send.run();
        rebuildWidgets();
    }

    private void useHeldBlock() {
        if (minecraft == null || minecraft.player == null || choice.part == null) {
            return;
        }
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.getItem() instanceof BlockItem item && (Materials.isFreeMaterial(item.getBlock())
                || item.getBlock().defaultBlockState().is(Materials.tag(choice.part.tag())))) {
            select(Materials.id(item.getBlock()));
        } else {
            message = Component.translatable("gui.realelevator.design.not_a_material");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        List<String> values = candidates();
        int columns = gridColumns();
        int rows = gridRows();
        scroll = Math.clamp(scroll, 0, Math.max(0, (values.size() + columns - 1) / columns - rows));
        String current = current();
        String hovered = null;
        for (int i = scroll * columns; i < values.size() && i < (scroll + rows) * columns; i++) {
            int x = gridLeft() + (i % columns) * SLOT;
            int y = gridTop() + (i / columns - scroll) * SLOT;
            String value = values.get(i);
            graphics.fill(x, y, x + SLOT, y + SLOT, value.equals(current) ? SELECTED : SLOT_BORDER);
            graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
            graphics.renderItem(new ItemStack(blockOf(value).asItem()), x + 1, y + 1);
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                hovered = value;
            }
        }
        if (form.preview() != null) {
            form.preview().render(graphics, previewLeft(), previewTop(), previewWidth(), previewHeight(), form, previewView, partialTick);
        }
        if (message != null) {
            graphics.drawCenteredString(font, message, width / 2, height - 40, 0xFFA0A0);
        }
        if (hovered != null) {
            graphics.renderTooltip(font, blockOf(hovered).getName(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<String> values = candidates();
        int columns = gridColumns();
        int column = (int) Math.floor((mouseX - gridLeft()) / SLOT);
        int row = (int) Math.floor((mouseY - gridTop()) / SLOT);
        if (button == 0 && column >= 0 && column < columns && row >= 0 && row < gridRows()) {
            int index = (row + scroll) * columns + column;
            if (index < values.size()) {
                select(values.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean overPreview(double mouseX, double mouseY) {
        return form.preview() != null && mouseX >= previewLeft() && mouseX <= previewLeft() + previewWidth()
                && mouseY >= previewTop() && mouseY <= previewTop() + previewHeight();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overPreview(mouseX, mouseY)) {
            form.preview().drag(dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overPreview(mouseX, mouseY)) {
            form.preview().zoom(scrollY);
            return true;
        }
        if (mouseX >= gridLeft()) {
            scroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
