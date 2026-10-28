package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.recipe.SpecificationCopyRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, RealElevator.MODID);

    /** 仕様書の複製。 */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<SpecificationCopyRecipe>> SPECIFICATION_COPY =
            RECIPE_SERIALIZERS.register("specification_copy", () -> new SimpleCraftingRecipeSerializer<>(SpecificationCopyRecipe::new));

    private ModRecipes() {
    }
}
