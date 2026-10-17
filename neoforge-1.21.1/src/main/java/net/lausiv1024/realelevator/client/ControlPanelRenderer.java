package net.lausiv1024.realelevator.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.block.ControlPanelBlockEntity;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.Cell;
import net.lausiv1024.realelevator.entity.CarShapes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 制御盤のプレビュー。設置する昇降路の範囲（水色）、各階に来るかごの外形（白）、邪魔なブロック（赤）を線で描く。
 */
public class ControlPanelRenderer implements BlockEntityRenderer<ControlPanelBlockEntity> {
    public ControlPanelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ControlPanelBlockEntity panel, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!panel.isPreviewing() || panel.getLevel() == null) {
            return;
        }
        BlockPos origin = panel.getBlockPos();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        ShaftLayout layout = panel.layout();

        AABB shaft = shaftBounds(panel, layout);
        LevelRenderer.renderLineBox(poseStack, lines, shaft.move(-origin.getX(), -origin.getY(), -origin.getZ()), 0.3F, 0.8F, 1.0F, 1.0F);

        Vec3 car = panel.carOrigin();
        for (int floorY : panel.floorYs()) {
            AABB box = CarShapes.toAabb(CarShapes.exterior(panel.model(), panel.facing()), car.x, floorY, car.z);
            LevelRenderer.renderLineBox(poseStack, lines, box.move(-origin.getX(), -origin.getY(), -origin.getZ()), 1.0F, 1.0F, 1.0F, 1.0F);
        }

        for (BlockPos pos : panel.obstructions(panel.getLevel())) {
            AABB box = new AABB(pos).inflate(0.002).move(-origin.getX(), -origin.getY(), -origin.getZ());
            LevelRenderer.renderLineBox(poseStack, lines, box, 1.0F, 0.2F, 0.2F, 1.0F);
        }
    }

    /** 設置する範囲（昇降路と三方枠）を囲む箱（ワールド座標）。 */
    private static AABB shaftBounds(ControlPanelBlockEntity panel, ShaftLayout layout) {
        List<Cell> cells = layout.requiredClearCells();
        AABB bounds = new AABB(panel.cellPos(cells.get(0)));
        for (Cell cell : cells) {
            bounds = bounds.minmax(new AABB(panel.cellPos(cell)));
        }
        return bounds;
    }

    @Override
    public boolean shouldRenderOffScreen(ControlPanelBlockEntity panel) {
        return panel.isPreviewing();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(ControlPanelBlockEntity panel) {
        if (!panel.isPreviewing()) {
            return new AABB(panel.getBlockPos());
        }
        return shaftBounds(panel, panel.layout()).inflate(2);
    }
}
