package dev.liquidcatmofu.particlemuffler.registry;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.liquidcatmofu.particlemuffler.menu.FilteredParticleMufflerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Particlemuffler.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<FilteredParticleMufflerMenu>> FILTERED_PARTICLE_MUFFLER = MENUS.register(
            "filtered_particle_muffler",
            () -> MenuRegistry.ofExtended(FilteredParticleMufflerMenu::new)
    );

    private ModMenus() {
    }

    public static void register() {
        MENUS.register();
    }
}
