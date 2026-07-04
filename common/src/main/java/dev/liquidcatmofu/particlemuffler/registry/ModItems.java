package dev.liquidcatmofu.particlemuffler.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Particlemuffler.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<BlockItem> PARTICLE_MUFFLER = ITEMS.register(
            "particle_muffler",
            () -> new BlockItem(ModBlocks.PARTICLE_MUFFLER.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
        CreativeTabRegistry.append(CreativeModeTabs.FUNCTIONAL_BLOCKS, PARTICLE_MUFFLER);
    }
}
