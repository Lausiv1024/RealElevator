package lausiv1024.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

public class BoxRenderer {
    public static void render(RenderType renderType, MatrixStack stack, IRenderTypeBuffer buffer,
                              Vector3f start, Vector3f end){
        float sx = start.x(), sy = start.y(), sz = start.z();
        float ex = end.x(), ey = end.y(), ez = end.z();
        drawQuad(renderType, stack, buffer,
                new Vector3f(ex, sy, ez), new Vector3f(sx, sy, ez), new Vector3f(sx, ey, ez), new Vector3f(ex, ey, ez));
        drawQuad(renderType, stack, buffer,
                new Vector3f(ex, sy, sz), new Vector3f(ex,sy,ez), new Vector3f(ex, ey, ez), new Vector3f(ex,ey,sz));
        drawQuad(renderType, stack, buffer,
                new Vector3f(sx, sy, ez), new Vector3f(sx, sy, sz), new Vector3f(sx, ey, sz), new Vector3f(sx, ey, ez));
        drawQuad(renderType, stack, buffer,
                new Vector3f(ex, ey, ez), new Vector3f(sx, ey, ez), new Vector3f(sx, ey, sz), new Vector3f(ex, ey, sz));
        drawQuad(renderType, stack, buffer,
                new Vector3f(ex, sy, sz), new Vector3f(sx, sy, sz), new Vector3f(sx, sy, ez), new Vector3f(ex, sy, ez));

    }
    private static void drawQuad(RenderType renderType, MatrixStack stack, IRenderTypeBuffer buffer,
                                 Vector3f vertex1, Vector3f vertex2, Vector3f vertex3, Vector3f vertex4){
        Matrix4f matrix4f = stack.last().pose();
        IVertexBuilder builder = buffer.getBuffer(renderType);
        builder.vertex(matrix4f, vertex1.x(), vertex1.y(), vertex1.z()).uv(1, 0).endVertex();
        builder.vertex(matrix4f, vertex2.x(), vertex2.y(), vertex2.z()).uv(0, 0).endVertex();
        builder.vertex(matrix4f, vertex3.x(), vertex3.y(), vertex3.z()).uv(0, 1).endVertex();
        builder.vertex(matrix4f, vertex4.x(), vertex4.y(), vertex4.z()).uv(1, 1).endVertex();
    }
}
