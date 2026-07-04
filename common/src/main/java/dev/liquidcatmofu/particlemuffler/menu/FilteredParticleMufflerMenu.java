package dev.liquidcatmofu.particlemuffler.menu;

import dev.liquidcatmofu.particlemuffler.blockentity.FilterMode;
import dev.liquidcatmofu.particlemuffler.blockentity.FilteredParticleMufflerBlockEntity;
import dev.liquidcatmofu.particlemuffler.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class FilteredParticleMufflerMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private FilterMode filterMode;
    private final List<ResourceLocation> particleIds;

    public FilteredParticleMufflerMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, buffer.readBlockPos(), FilterMode.byName(buffer.readUtf()), readParticleIds(buffer));
    }

    public FilteredParticleMufflerMenu(int containerId, BlockPos blockPos, FilterMode filterMode, List<ResourceLocation> particleIds) {
        super(ModMenus.FILTERED_PARTICLE_MUFFLER.get(), containerId);
        this.blockPos = blockPos;
        this.filterMode = filterMode;
        this.particleIds = new ArrayList<>(particleIds);
    }

    public static FilteredParticleMufflerMenu fromBlockEntity(int containerId, FilteredParticleMufflerBlockEntity blockEntity) {
        return new FilteredParticleMufflerMenu(containerId, blockEntity.getBlockPos(), blockEntity.getFilterMode(), List.copyOf(blockEntity.getParticleIds()));
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public List<ResourceLocation> getParticleIds() {
        return List.copyOf(particleIds);
    }

    public void setLocalFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
    }

    public void setLocalParticleIds(List<ResourceLocation> particleIds) {
        this.particleIds.clear();
        this.particleIds.addAll(particleIds);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static List<ResourceLocation> readParticleIds(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ResourceLocation> particleIds = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            particleIds.add(buffer.readResourceLocation());
        }
        return particleIds;
    }
}
