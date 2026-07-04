package dev.liquidcatmofu.particlemuffler.blockentity;

import dev.liquidcatmofu.particlemuffler.client.ParticleMufflerClientRegistry;
import dev.liquidcatmofu.particlemuffler.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ParticleMufflerBlockEntity extends BlockEntity {
    private static final String SECTION_RADIUS_TAG = "SectionRadius";
    private static final String ENABLED_TAG = "Enabled";
    private static final int DEFAULT_SECTION_RADIUS = 0;
    private static final int MAX_SECTION_RADIUS = 0;

    private int sectionRadius = DEFAULT_SECTION_RADIUS;
    private boolean enabled = true;

    public ParticleMufflerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARTICLE_MUFFLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ParticleMufflerBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.updateClientRegistry();
        }
    }

    public int getSectionRadius() {
        return sectionRadius;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        setChangedAndSync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(SECTION_RADIUS_TAG, sectionRadius);
        tag.putBoolean(ENABLED_TAG, enabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int savedSectionRadius = tag.contains(SECTION_RADIUS_TAG) ? tag.getInt(SECTION_RADIUS_TAG) : DEFAULT_SECTION_RADIUS;
        sectionRadius = Mth.clamp(savedSectionRadius, 0, MAX_SECTION_RADIUS);
        enabled = !tag.contains(ENABLED_TAG) || tag.getBoolean(ENABLED_TAG);
        updateClientRegistry();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            ParticleMufflerClientRegistry.remove(worldPosition);
        }
        super.setRemoved();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        updateClientRegistry();
    }

    private void updateClientRegistry() {
        if (level == null || !level.isClientSide) {
            return;
        }

        ParticleMufflerClientRegistry.addOrUpdate(worldPosition, sectionRadius, enabled);
    }
}
