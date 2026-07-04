package dev.liquidcatmofu.particlemuffler.network;

import dev.architectury.networking.NetworkManager;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.liquidcatmofu.particlemuffler.blockentity.FilterMode;
import dev.liquidcatmofu.particlemuffler.blockentity.FilteredParticleMufflerBlockEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ParticleMufflerNetworking {
    public static final ResourceLocation UPDATE_FILTERED_MUFFLER = new ResourceLocation(Particlemuffler.MOD_ID, "update_filtered_muffler");
    private static final int MAX_PARTICLE_IDS = 128;

    private ParticleMufflerNetworking() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), UPDATE_FILTERED_MUFFLER, ParticleMufflerNetworking::receiveUpdateFilteredMuffler);
    }

    public static void writeUpdateFilteredMuffler(FriendlyByteBuf buffer, BlockPos pos, FilterMode filterMode, Iterable<ResourceLocation> particleIds) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(filterMode.name());

        int size = 0;
        for (ResourceLocation ignored : particleIds) {
            size++;
        }

        buffer.writeVarInt(size);
        for (ResourceLocation particleId : particleIds) {
            buffer.writeResourceLocation(particleId);
        }
    }

    private static void receiveUpdateFilteredMuffler(FriendlyByteBuf buffer, NetworkManager.PacketContext context) {
        BlockPos pos = buffer.readBlockPos();
        FilterMode filterMode = FilterMode.byName(buffer.readUtf());
        int count = buffer.readVarInt();
        Set<ResourceLocation> particleIds = new HashSet<>();
        for (int index = 0; index < count; index++) {
            ResourceLocation particleId = buffer.readResourceLocation();
            if (particleIds.size() < MAX_PARTICLE_IDS) {
                particleIds.add(particleId);
            }
        }

        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                return;
            }

            if (player.level().getBlockEntity(pos) instanceof FilteredParticleMufflerBlockEntity blockEntity) {
                blockEntity.setFilterMode(filterMode);
                blockEntity.setParticleIds(particleIds);
            }
        });
    }
}
