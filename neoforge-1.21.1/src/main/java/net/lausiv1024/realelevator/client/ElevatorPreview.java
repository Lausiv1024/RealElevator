package net.lausiv1024.realelevator.client;

import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.car.FloorEntrances;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 設定画面の中に、今の設定のかご内と乗場を描くプレビュー。
 *
 * <p>画面の中だけで使うかごと乗場の扉のエンティティ（ワールドには加えない）を今の設定から作り、ふだんの描画で描く。
 * かご内は奥の壁の前から扉の方を見て、乗場は正面から見る。ドラッグで左右に一周、上下に回せる（かご内は見回し、乗場は回り込む）。
 * ホイールで拡大・縮小する（視野を狭める・広げる）。
 * 視野は横の広さ（{@value #HORIZONTAL_FOV}°を拡大率で割ったもの）で決め、範囲の縦横比によらず横に見える広さをそろえる。
 * 表示器には最下階（またはその乗場の階）の階名と上の矢印を出し、ホールランタンと乗場の▲ボタンを点ける。
 */
final class ElevatorPreview {
    /** 見るもの。 */
    enum View {
        CAR,
        LANDING
    }

    /**
     * かごを置く位置。
     *
     * @param carPos かごの原点（床面の中心）。y はプレビューで見せる階の床面
     * @param facing かごの向き（乗場の向き）
     */
    record Anchor(Vec3 carPos, Direction facing) {
    }

    /** かご内で目を置く高さ [ブロック]。 */
    private static final double EYE_HEIGHT = 1.55;
    /** かご内で、奥の壁から目までの距離 [ブロック]。 */
    private static final double EYE_FROM_BACK = 0.25;
    /** 乗場で、乗場の面から目までの距離 [ブロック]。 */
    private static final double LANDING_DISTANCE = 3.2;
    /** 横の視野 [°]。縦の視野は範囲の縦横比から決める（{@value #MAX_VERTICAL_FOV}°まで）。 */
    private static final double HORIZONTAL_FOV = 80;
    private static final double MAX_VERTICAL_FOV = 110;
    private static final float DRAG_DEGREES_PER_PIXEL = 0.5F;
    /** 上下に回せる範囲 [°]。かご内は見上げ・見下ろしの角度、乗場は見下ろす（見上げる）位置の高さの角度。 */
    private static final float MAX_PITCH = 85;
    /** ホイール1目盛りの拡大率の変化と、拡大率の範囲。 */
    private static final double ZOOM_STEP = 1.15;
    private static final double MIN_ZOOM = 0.7;
    private static final double MAX_ZOOM = 4;
    /** かご内の、はじめの見上げる角度 [°]（バニラの決まりで、負が上）。 */
    private static final float CAR_PITCH = -8;
    private static final int BACKGROUND = 0xFF101418;
    private static final int BORDER = 0xFF555555;

    private final Supplier<CompoundTag> installSettings;
    private final Supplier<Anchor> anchor;
    private float yaw;
    /** 上下の角度 [°]。かご内は見下ろすほど、乗場は高いところから見るほど大きい。 */
    private float pitch;
    private double zoom = 1;
    private View lastView;
    /** 乗場を見せる停止階。null なら置く位置の階。 */
    @Nullable
    private Integer shownFloor;
    private ElevatorCarEntity car;
    private LandingDoorEntity door;
    private CompoundTag builtFrom;

    /**
     * @param installSettings 設置する前だけ変えられる設定（機種・扉・二方向出入口・展望仕様、出入口）。設定画面の設定の形
     * @param anchor          かごを置く位置
     */
    ElevatorPreview(Supplier<CompoundTag> installSettings, Supplier<Anchor> anchor) {
        this.installSettings = installSettings;
        this.anchor = anchor;
    }

    /** ドラッグした（dx, dy [px]）。左右で一周、上下は {@value #MAX_PITCH}°まで回す。 */
    void drag(double dx, double dy) {
        yaw = (float) ((yaw + dx * DRAG_DEGREES_PER_PIXEL) % 360);
        pitch = (float) Math.clamp(pitch + dy * DRAG_DEGREES_PER_PIXEL, -MAX_PITCH, MAX_PITCH);
    }

    /** ホイールを回した（上へ回すと拡大）。 */
    void zoom(double scroll) {
        zoom = Math.clamp(zoom * Math.pow(ZOOM_STEP, Math.signum(scroll)), MIN_ZOOM, MAX_ZOOM);
    }

    /** 画面の (x, y) から w × h の範囲に、form の今の設定で view を描く。 */
    void render(GuiGraphics graphics, int x, int y, int w, int h, SettingsForm form, View view, float partialTick) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDER);
        graphics.fill(x, y, x + w, y + h, BACKGROUND);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || w <= 0 || h <= 0) {
            return;
        }
        if (view != lastView) {
            lastView = view;
            yaw = 0;
            pitch = 0;
            zoom = 1;
        }
        Anchor where = anchor.get();
        rebuildIfChanged(minecraft.level, form, where);
        if (car == null) {
            return;
        }
        // GUI のこれまでの描画を先に流してから、範囲の中だけに遠近のある絵を描く
        graphics.flush();
        Window window = minecraft.getWindow();
        double scale = window.getGuiScale();
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        RenderSystem.viewport((int) (x * scale), (int) (window.getHeight() - (y + h) * scale), (int) (w * scale), (int) (h * scale));
        double verticalFov = Math.min(MAX_VERTICAL_FOV,
                Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(HORIZONTAL_FOV / zoom / 2)) * h / w)));
        RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(verticalFov), (float) w / h, 0.05F, 64F),
                VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        Lighting.setupLevel();
        try {
            renderScene(minecraft, where, view, partialTick);
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            Lighting.setupFor3DItems();
        }
    }

    private void renderScene(Minecraft minecraft, Anchor where, View view, float partialTick) {
        CarSpec spec = car.spec();
        int turns = where.facing().get2DDataValue();
        Vec3 eye;
        float viewYaw;
        float viewPitch;
        if (view == View.CAR) {
            // 奥の壁の前から、扉の方（かごのローカル座標の +Z）を見回す
            Box interior = CarGeometry.interior(spec);
            eye = local(where, turns, 0, EYE_HEIGHT, interior.minZ() + EYE_FROM_BACK);
            viewYaw = yawToward(rotate(0, 1, turns)) + yaw;
            viewPitch = Math.clamp(CAR_PITCH + pitch, -89, 89);
        } else {
            // 乗場の正面から、乗場の面のまわりを回り込む（上下は、見る位置の高さを変える）
            double face = DoorGeometry.landingFace(spec);
            double angle = Math.toRadians(yaw);
            double elevation = Math.toRadians(pitch);
            double flat = Math.cos(elevation) * LANDING_DISTANCE;
            eye = local(where, turns, -Math.sin(angle) * flat, EYE_HEIGHT + Math.sin(elevation) * LANDING_DISTANCE,
                    face + Math.cos(angle) * flat);
            Vec3 target = local(where, turns, 0, EYE_HEIGHT, face);
            Vec3 toward = target.subtract(eye);
            viewYaw = yawToward(toward);
            viewPitch = (float) -Math.toDegrees(Math.atan2(toward.y, Math.sqrt(toward.x * toward.x + toward.z * toward.z)));
        }
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.XP.rotationDegrees(viewPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(viewYaw + 180));
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        for (Entity entity : new Entity[] {car, door}) {
            dispatcher.render(entity, entity.getX() - eye.x, entity.getY() - eye.y, entity.getZ() - eye.z, 0, partialTick, poseStack, buffers,
                    LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    /** かごのローカル座標 (x, y, z) の点のワールドの位置。 */
    private static Vec3 local(Anchor where, int turns, double x, double y, double z) {
        Vec3 rotated = rotate(x, z, turns);
        return where.carPos().add(rotated.x, y, rotated.z);
    }

    /** 水平の (x, z) を、かごのローカル座標と同じ決まりで90°ずつ回す（(x, z) → (−z, x)）。 */
    private static Vec3 rotate(double x, double z, int turns) {
        for (int i = 0; i < turns; i++) {
            double t = x;
            x = -z;
            z = t;
        }
        return new Vec3(x, 0, z);
    }

    /** 向き direction を見るときのヨー（バニラの決まり：0 で +Z、90 で −X）。 */
    private static float yawToward(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    /**
     * 乗場を見せる停止階（0始まり）を変える。null なら、置く位置の階（どの停止階でもなければ最下階）。
     * デザインの画面で階ごとの乗場の意匠を選んでいるとき、その意匠の階を見せる。
     */
    void showFloor(@Nullable Integer floor) {
        shownFloor = floor;
    }

    /** 設定か置く位置が変わっていれば、かごと乗場の扉を作り直す。 */
    private void rebuildIfChanged(Level level, SettingsForm form, Anchor where) {
        CompoundTag key = installSettings.get().copy();
        form.writeTo(key);
        key.putInt("ShownFloor", shownFloor != null ? shownFloor : -1);
        key.putDouble("AnchorX", where.carPos().x);
        key.putDouble("AnchorY", where.carPos().y);
        key.putDouble("AnchorZ", where.carPos().z);
        key.putString("AnchorFacing", where.facing().getName());
        if (key.equals(builtFrom)) {
            return;
        }
        builtFrom = key;
        build(level, key, form, where);
    }

    private void build(Level level, CompoundTag settings, SettingsForm form, Anchor where) {
        car = ModEntities.ELEVATOR_CAR.get().create(level);
        door = ModEntities.LANDING_DOOR.get().create(level);
        if (car == null || door == null) {
            car = null;
            return;
        }
        CarModel model = ConfigTags.parseEnum(CarModel.class, settings.getString("Model"), CarModel.P11);
        DoorType doorType = ConfigTags.parseEnum(DoorType.class, settings.getString("Door"), DoorType.CENTER);
        CarSpec spec = doorType.isAvailable(model) ? new CarSpec(model, doorType) : CarSpec.of(model);
        int floorY = (int) Math.floor(where.carPos().y);
        // 停止階がまだなければ、見せる階と、その上の階を仮に置く
        int[] floors = form.floors().length >= 2 ? form.floors() : new int[] {floorY, floorY + 5};
        car.setup(spec, where.facing(), floors, SpeedSetting.standard(SpeedSpec.NORMAL_LOW));
        if (settings.getBoolean("Through")) {
            FloorEntrances[] entrances = new FloorEntrances[car.floors().length];
            for (int i = 0; i < entrances.length; i++) {
                entrances[i] = form.entrance(car.floors()[i]);
            }
            car.setThrough(entrances);
        }
        car.setObservation(settings.getBoolean("Observation"));
        car.setFixtures(form.fixtures());
        car.setInterior(form.interior());
        String[] labels = new String[car.floors().length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = form.label(car.floors()[i]);
        }
        car.setLabels(labels);
        car.setPreview(true);
        // 見せる階がどの停止階でもなければ、最下階を見せる
        int shown = car.floorIndexAt(floorY) >= 0 ? floorY : car.floors()[0];
        if (shownFloor != null && shownFloor >= 0 && shownFloor < car.floors().length) {
            shown = car.floors()[shownFloor];
        }
        car.moveTo(where.carPos().x, shown, where.carPos().z, 0, 0);
        door.setup(car, Entrance.FRONT);
        door.moveTo(where.carPos().x, shown, where.carPos().z, 0, 0);
        door.setPreviewCar(car);
    }
}
