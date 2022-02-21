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
		bb_main.texOffs(0, 0).addBox(-25.0F, -60.0F, -28.0F, 50.0F, 4.0F, 56.0F, 0.0F, false);
		bb_main.texOffs(0, 60).addBox(-21.0F, -56.0F, -24.0F, 42.0F, 3.0F, 48.0F, 0.0F, false);
		bb_main.texOffs(30, 127).addBox(-26.0F, -78.0F, -13.0F, 5.0F, 18.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(0, 60).addBox(21.0F, -78.0F, -13.0F, 5.0F, 18.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(30, 28).addBox(24.0F, -79.0F, -11.0F, 3.0F, 4.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(8, 6).addBox(27.0F, -79.0F, -11.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(8, 0).addBox(27.0F, -79.0F, -7.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 6).addBox(-29.0F, -79.0F, -11.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-29.0F, -79.0F, -7.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(28, 0).addBox(-27.0F, -79.0F, -11.0F, 3.0F, 4.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(0, 111).addBox(-27.0F, -65.0F, -29.0F, 54.0F, 14.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 28).addBox(12.0F, -68.0F, -27.0F, 8.0F, 8.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(112, 111).addBox(-21.0F, -68.0F, -11.0F, 42.0F, 8.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(48, 0).addBox(-22.0F, -78.0F, 25.0F, 1.0F, 18.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(44, 10).addBox(21.0F, -78.0F, 25.0F, 1.0F, 18.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(58, 127).addBox(21.0F, -76.0F, -3.0F, 1.0F, 1.0F, 28.0F, 0.0F, false);
		bb_main.texOffs(0, 127).addBox(-22.0F, -76.0F, -3.0F, 1.0F, 1.0F, 28.0F, 0.0F, false);
		bb_main.texOffs(112, 125).addBox(-21.0F, -76.0F, 25.0F, 42.0F, 1.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-22.0F, -72.0F, 0.0F, 6.0F, 12.0F, 16.0F, 0.0F, false);
		bb_main.texOffs(0, 50).addBox(-16.0F, -61.0F, 2.0F, 16.0F, 1.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(37, 43).addBox(-2.0F, -61.0F, -5.0F, 2.0F, 1.0F, 7.0F, 0.0F, false);
		bb_main.texOffs(0, 12).addBox(13.0F, -61.0F, -13.0F, 2.0F, 1.0F, 2.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {

	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float p_225598_5_, float p_225598_6_, float p_225598_7_, float p_225598_8_) {
		bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
	}
}