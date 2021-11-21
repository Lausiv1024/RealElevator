package lausiv1024.client.render.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import lausiv1024.tileentity.ElevatorPartTE;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

public abstract class ElevatorPartRender<T extends ElevatorPartTE> extends TileEntityRenderer<T> {
    protected T tile;
    protected float partialTicks = 0;
    protected MatrixStack matrixStack;
    protected IRenderTypeBuffer buffer;
    protected int combinedLight;
    protected int combinedOverlay;

    public ElevatorPartRender(TileEntityRendererDispatcher tileEntityRendererDispatcher) {
        super(tileEntityRendererDispatcher);
    }

    @Override
    public void render(T tile, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        if (tile.getLevel() == null){
            return;
        }

        this.tile = tile;
        this.partialTicks = partialTicks;
        this.matrixStack = stack;
        this.buffer = buffer;
        this.combinedLight = combinedLight;
        this.combinedOverlay = combinedOverlay;

        this.render();
    }

    protected abstract void render();
}
