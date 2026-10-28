package net.lausiv1024.realelevator.recipe;

import net.lausiv1024.realelevator.design.ElevatorProfile;
import net.lausiv1024.realelevator.item.SpecificationItem;
import net.lausiv1024.realelevator.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 仕様書の複製。書いた仕様書と白紙の仕様書を並べると、白紙のほうに同じ内容を書く（書いた仕様書は作業台に残る）。 */
public class SpecificationCopyRecipe extends CustomRecipe {
    public SpecificationCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return original(input) >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int original = original(input);
        if (original < 0) {
            return ItemStack.EMPTY;
        }
        return input.getItem(original).copyWithCount(1);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int original = original(input);
        if (original >= 0) {
            remaining.set(original, input.getItem(original).copyWithCount(1));
        }
        return remaining;
    }

    /** 書いた仕様書の場所。書いた仕様書1つと白紙1つだけが並んでいなければ −1。 */
    private static int original(CraftingInput input) {
        int written = -1;
        int blank = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof SpecificationItem)) {
                return -1;
            }
            if (ElevatorProfile.isPresent(SpecificationItem.profile(stack))) {
                if (written >= 0) {
                    return -1;
                }
                written = i;
            } else {
                blank++;
            }
        }
        return blank == 1 ? written : -1;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SPECIFICATION_COPY.get();
    }
}
