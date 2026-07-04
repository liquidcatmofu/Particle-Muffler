package dev.liquidcatmofu.particlemuffler.client;

import dev.liquidcatmofu.particlemuffler.blockentity.FilterMode;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record ClientMufflerEntry(
        BlockPos pos,
        int sectionRadius,
        boolean enabled,
        FilterMode filterMode,
        Set<ResourceLocation> particleIds
) {
    public boolean isFiltered() {
        return filterMode != null;
    }
}
