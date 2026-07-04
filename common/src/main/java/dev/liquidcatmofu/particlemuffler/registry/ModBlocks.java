package dev.liquidcatmofu.particlemuffler.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.liquidcatmofu.particlemuffler.block.ParticleMufflerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Particlemuffler.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<ParticleMufflerBlock> PARTICLE_MUFFLER = BLOCKS.register(
            "particle_muffler",
            () -> new ParticleMufflerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
    }
}
