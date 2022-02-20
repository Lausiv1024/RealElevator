package lausiv1024.client.entitymodel;

// Made with Blockbench 4.1.5
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class WallStandard extends EntityModel<Entity> {
	private final ModelRenderer bb_main;

	public WallStandard() {
		texWidth = 256;
		texHeight = 256;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(161, 117).addBox(12.0F, -56.0F, -28.0F, 12.0F, 56.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(161, 59).addBox(-24.0F, -56.0F, -28.0F, 12.0F, 56.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(55, 56).addBox(22.0F, -56.0F, -26.0F, 2.0F, 56.0F, 51.0F, 0.0F, false);
		bb_main.texOffs(55, 0).addBox(-12.0F, -56.0F, -28.0F, 24.0F, 8.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(24, 107).addBox(24.0F, -60.0F, -12.0F, 2.0F, 60.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(62, 163).addBox(-25.0F, -56.0F, -18.0F, 1.0F, 56.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(58, 163).addBox(24.0F, -56.0F, -18.0F, 1.0F, 56.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-24.0F, -56.0F, -26.0F, 2.0F, 56.0F, 51.0F, 0.0F, false);
		bb_main.texOffs(110, 0).addBox(-25.0F, -56.0F, 25.0F, 50.0F, 56.0F, 3.0F, 0.0F, false);
		bb_main.texOffs(0, 107).addBox(-26.0F, -60.0F, -12.0F, 2.0F, 60.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(48, 159).addBox(-25.0F, -56.0F, 6.0F, 1.0F, 56.0F, 4.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {

	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float p_225598_5_, float p_225598_6_, float p_225598_7_, float p_225598_8_) {
		bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
	}
}