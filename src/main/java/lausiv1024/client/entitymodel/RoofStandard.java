package lausiv1024.client.entitymodel;

// Made with Blockbench 4.1.5
// Exported for Minecraft version 1.15 - 1.16 with MCP mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class RoofStandard extends EntityModel<Entity> {
	private final ModelRenderer bb_main;

	public RoofStandard() {
		texWidth = 256;
		texHeight = 256;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(30, 129).addBox(-26.0F, -78.0F, -12.0F, 5.0F, 18.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(0, 129).addBox(21.0F, -78.0F, -12.0F, 5.0F, 18.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(0, 10).addBox(24.0F, -79.0F, -10.0F, 4.0F, 4.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(29, 6).addBox(28.0F, -79.0F, -10.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(29, 0).addBox(28.0F, -79.0F, -6.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(12, 28).addBox(-30.0F, -79.0F, -10.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(4, 28).addBox(-30.0F, -79.0F, -6.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-28.0F, -79.0F, -10.0F, 4.0F, 4.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(100, 128).addBox(12.0F, -68.0F, -27.0F, 8.0F, 8.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(114, 112).addBox(-21.0F, -68.0F, -10.0F, 42.0F, 8.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(0, 28).addBox(-22.0F, -78.0F, 25.0F, 1.0F, 18.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(20, 0).addBox(21.0F, -78.0F, 25.0F, 1.0F, 18.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(0, 28).addBox(21.0F, -76.0F, -2.0F, 1.0F, 1.0F, 27.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-22.0F, -76.0F, -2.0F, 1.0F, 1.0F, 27.0F, 0.0F, false);
		bb_main.texOffs(114, 126).addBox(-21.0F, -76.0F, 25.0F, 42.0F, 1.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(0, 60).addBox(-22.0F, -72.0F, 0.0F, 6.0F, 12.0F, 16.0F, 0.0F, false);
		bb_main.texOffs(0, 88).addBox(-16.0F, -61.0F, 2.0F, 16.0F, 1.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 20).addBox(-2.0F, -61.0F, -4.0F, 2.0F, 1.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(17, 19).addBox(13.0F, -61.0F, -13.0F, 2.0F, 1.0F, 3.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-25.0F, -60.0F, -28.0F, 50.0F, 4.0F, 56.0F, 0.0F, false);
		bb_main.texOffs(0, 60).addBox(-21.0F, -56.0F, -25.0F, 42.0F, 3.0F, 49.0F, 0.0F, false);
		bb_main.texOffs(0, 112).addBox(-27.0F, -65.0F, -30.0F, 54.0F, 14.0F, 3.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {

	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float p_225598_5_, float p_225598_6_, float p_225598_7_, float p_225598_8_) {
		bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
	}
}