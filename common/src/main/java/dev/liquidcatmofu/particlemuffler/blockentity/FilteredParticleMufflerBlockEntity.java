package dev.liquidcatmofu.particlemuffler.blockentity;

import dev.liquidcatmofu.particlemuffler.client.ParticleMufflerClientRegistry;
import dev.liquidcatmofu.particlemuffler.registry.ModBlockEntities;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class FilteredParticleMufflerBlockEntity extends ParticleMufflerBlockEntity {
    private static final String FILTER_MODE_TAG = "FilterMode";
    private static final String PARTICLE_IDS_TAG = "ParticleIds";

    private FilterMode filterMode = FilterMode.BLACKLIST;
    private final Set<ResourceLocation> particleIds = new HashSet<>();

    public FilteredParticleMufflerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTERED_PARTICLE_MUFFLER.get(), pos, state);
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public Set<ResourceLocation> getParticleIds() {
        return Collections.unmodifiableSet(particleIds);
    }

    public void setFilterMode(FilterMode filterMode) {
        if (this.filterMode == filterMode) {
            return;
        }

        this.filterMode = filterMode;
        setChangedAndSync();
    }

    public void setParticleIds(Set<ResourceLocation> particleIds) {
        if (this.particleIds.equals(particleIds)) {
            return;
        }

        this.particleIds.clear();
        this.particleIds.addAll(particleIds);
        setChangedAndSync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(FILTER_MODE_TAG, filterMode.name());

        ListTag particleIdTags = new ListTag();
        for (ResourceLocation particleId : particleIds) {
            particleIdTags.add(StringTag.valueOf(particleId.toString()));
        }
        tag.put(PARTICLE_IDS_TAG, particleIdTags);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        filterMode = tag.contains(FILTER_MODE_TAG) ? FilterMode.byName(tag.getString(FILTER_MODE_TAG)) : FilterMode.BLACKLIST;
        particleIds.clear();

        ListTag particleIdTags = tag.getList(PARTICLE_IDS_TAG, Tag.TAG_STRING);
        for (int index = 0; index < particleIdTags.size(); index++) {
            ResourceLocation particleId = ResourceLocation.tryParse(particleIdTags.getString(index));
            if (particleId != null) {
                particleIds.add(particleId);
            }
        }

        updateClientRegistry();
    }

    @Override
    protected void updateClientRegistry() {
        if (level == null || !level.isClientSide) {
            return;
        }

        ParticleMufflerClientRegistry.addOrUpdateFiltered(worldPosition, getSectionRadius(), isEnabled(), filterMode, particleIds);
    }
}
