package dev.liquidcatmofu.particlemuffler.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.liquidcatmofu.particlemuffler.blockentity.FilteredParticleMufflerBlockEntity;
import dev.liquidcatmofu.particlemuffler.blockentity.ParticleMufflerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Particlemuffler.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<ParticleMufflerBlockEntity>> PARTICLE_MUFFLER = BLOCK_ENTITIES.register(
            "particle_muffler",
            () -> BlockEntityType.Builder.of(ParticleMufflerBlockEntity::new, ModBlocks.PARTICLE_MUFFLER.get()).build(null)
    );

    public static final RegistrySupplier<BlockEntityType<FilteredParticleMufflerBlockEntity>> FILTERED_PARTICLE_MUFFLER = BLOCK_ENTITIES.register(
            "filtered_particle_muffler",
            () -> BlockEntityType.Builder.of(FilteredParticleMufflerBlockEntity::new, ModBlocks.FILTERED_PARTICLE_MUFFLER.get()).build(null)
    );

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
