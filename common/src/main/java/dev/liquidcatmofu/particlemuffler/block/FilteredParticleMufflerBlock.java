package dev.liquidcatmofu.particlemuffler.block;

import com.mojang.serialization.MapCodec;
import dev.liquidcatmofu.particlemuffler.blockentity.FilteredParticleMufflerBlockEntity;
import dev.liquidcatmofu.particlemuffler.blockentity.ParticleMufflerBlockEntity;
import dev.liquidcatmofu.particlemuffler.registry.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class FilteredParticleMufflerBlock extends ParticleMufflerBlock {
    public static final MapCodec<FilteredParticleMufflerBlock> CODEC = simpleCodec(FilteredParticleMufflerBlock::new);

    public FilteredParticleMufflerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilteredParticleMufflerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.FILTERED_PARTICLE_MUFFLER.get(), ParticleMufflerBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FilteredParticleMufflerBlockEntity blockEntity) {
            MenuRegistry.openExtendedMenu(serverPlayer, blockEntity);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
