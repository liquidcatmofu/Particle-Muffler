package dev.liquidcatmofu.particlemuffler.block;

import dev.liquidcatmofu.particlemuffler.blockentity.FilteredParticleMufflerBlockEntity;
import dev.liquidcatmofu.particlemuffler.blockentity.ParticleMufflerBlockEntity;
import dev.liquidcatmofu.particlemuffler.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class FilteredParticleMufflerBlock extends ParticleMufflerBlock {
    public FilteredParticleMufflerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilteredParticleMufflerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.FILTERED_PARTICLE_MUFFLER.get(), ParticleMufflerBlockEntity::tick);
    }
}
