package dev.liquidcatmofu.particlemuffler.client;

import net.minecraft.core.BlockPos;

public record ClientMufflerEntry(BlockPos pos, int sectionRadius, boolean enabled) {
}
