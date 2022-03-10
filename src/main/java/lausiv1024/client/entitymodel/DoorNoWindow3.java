package lausiv1024.client.entitymodel;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class DoorNoWindow3 extends EntityModel<Entity> {
    private final ModelRenderer bb_main;

    public DoorNoWindow3(){
        texWidth = 64;
        texHeight = 64;

        bb_main = new ModelRenderer(this);
        bb_main.setPos(0.0F, 24.0F, 0.0F);
        bb_main.texOffs(0, 0).addBox(-6.0F, -48.0F, -1.0F, 12.0F, 48.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(34, 8).addBox(-5.0F, -3.0F, 0.0F, 10.0F, 3.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(30, 0).addBox(5.0F, -48.0F, 0.0F, 1.0F, 48.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(26, 0).addBox(-6.0F, -48.0F, 0.0F, 1.0F, 48.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(34, 4).addBox(-5.0F, -48.0F, 0.0F, 10.0F, 3.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(34, 0).addBox(-5.0F, -51.0F, 0.0F, 10.0F, 3.0F, 1.0F, 0.0F, false);

    }

    @Override
    public void setupAnim(Entity entity, float v, float v1, float v2, float v3, float v4) {

    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float p_225598_5_, float p_225598_6_, float p_225598_7_, float p_225598_8_) {
        bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
    }
}
