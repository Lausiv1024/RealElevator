package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.menu.InstallerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, RealElevator.MODID);

    /** 制御盤の設定画面。開くときに制御盤の位置を送る。 */
    public static final DeferredHolder<MenuType<?>, MenuType<InstallerMenu>> INSTALLER =
            MENU_TYPES.register("installer", () -> IMenuTypeExtension.create(InstallerMenu::new));

    private ModMenus() {
    }
}
