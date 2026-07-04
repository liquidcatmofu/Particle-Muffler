package dev.liquidcatmofu.particlemuffler.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Particlemuffler.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<BlockItem> PARTICLE_MUFFLER = ITEMS.register(
            "particle_muffler",
            () -> new TooltipBlockItem(ModBlocks.PARTICLE_MUFFLER.get(), new Item.Properties(), "particle_muffler")
    );

    public static final RegistrySupplier<BlockItem> FILTERED_PARTICLE_MUFFLER = ITEMS.register(
            "filtered_particle_muffler",
            () -> new TooltipBlockItem(ModBlocks.FILTERED_PARTICLE_MUFFLER.get(), new Item.Properties(), "filtered_particle_muffler")
    );

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
        CreativeTabRegistry.append(CreativeModeTabs.FUNCTIONAL_BLOCKS, PARTICLE_MUFFLER);
        CreativeTabRegistry.append(CreativeModeTabs.FUNCTIONAL_BLOCKS, FILTERED_PARTICLE_MUFFLER);
    }

    private static final class TooltipBlockItem extends BlockItem {
        private final String tooltipKey;

        private TooltipBlockItem(net.minecraft.world.level.block.Block block, Properties properties, String tooltipKey) {
            super(block, properties);
            this.tooltipKey = tooltipKey;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.particlemuffler." + tooltipKey + ".range"));
            tooltip.add(Component.translatable("tooltip.particlemuffler." + tooltipKey + ".redstone"));
        }
    }
}
