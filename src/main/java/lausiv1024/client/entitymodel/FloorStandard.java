package lausiv1024.client.entitymodel;

// Made with Blockbench 4.1.5
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class FloorStandard extends EntityModel<Entity> {
	private final ModelRenderer bb_main;

	public FloorStandard() {
		texWidth = 256;
		texHeight = 256;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(0, 0).addBox(-26.0F, 0.0F, -27.0F, 52.0F, 6.0F, 55.0F, 0.0F, false);
		bb_main.texOffs(0, 85).addBox(-27.0F, 0.0F, -29.0F, 54.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(100, 91).addBox(-14.0F, 3.0F, -28.0F, 28.0F, 24.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(0, 91).addBox(-21.0F, 6.0F, -12.0F, 42.0F, 6.0F, 8.0F, 0.0F, false);
		bb_main.texOffs(0, 20).addBox(21.0F, 6.0F, -13.0F, 5.0F, 10.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-26.0F, 6.0F, -13.0F, 5.0F, 10.0F, 10.0F, 0.0F, false);
		bb_main.texOffs(20, 20).addBox(-27.0F, 14.0F, -11.0F, 3.0F, 4.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(20, 0).addBox(24.0F, 14.0F, -11.0F, 3.0F, 4.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(30, 30).addBox(27.0F, 14.0F, -11.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(30, 10).addBox(27.0F, 14.0F, -7.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 20).addBox(-29.0F, 14.0F, -11.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-29.0F, 14.0F, -7.0F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 61).addBox(-30.0F, 6.0F, 7.0F, 60.0F, 16.0F, 8.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {

	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float p_225598_5_, float p_225598_6_, float p_225598_7_, float p_225598_8_) {
		bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
	}
}