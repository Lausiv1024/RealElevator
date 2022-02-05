package lausiv1024.bakedmodel;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.extensions.IForgeUnbakedModel;
import net.minecraftforge.client.model.SeparatePerspectiveModel;

public class CwtRopeWheelModel extends SeparatePerspectiveModel.BakedModel implements IForgeBakedModel {
    public CwtRopeWheelModel(boolean isAmbientOcclusion, boolean isGui3d, boolean isSideLit, TextureAtlasSprite particle, ItemOverrideList overrides, IBakedModel baseModel, ImmutableMap<ItemCameraTransforms.TransformType, IBakedModel> perspectives) {
        super(isAmbientOcclusion, isGui3d, isSideLit, particle, overrides, baseModel, perspectives);
    }



}
