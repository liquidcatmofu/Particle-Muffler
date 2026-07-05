package dev.liquidcatmofu.particlemuffler.network;

import dev.architectury.networking.NetworkManager;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.liquidcatmofu.particlemuffler.blockentity.FilterMode;
import dev.liquidcatmofu.particlemuffler.blockentity.FilteredParticleMufflerBlockEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ParticleMufflerNetworking {
    private static final ResourceLocation UPDATE_FILTERED_MUFFLER_ID = ResourceLocation.fromNamespaceAndPath(Particlemuffler.MOD_ID, "update_filtered_muffler");
    private static final int MAX_PARTICLE_IDS = 128;

    private ParticleMufflerNetworking() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), UpdateFilteredMufflerPayload.TYPE, UpdateFilteredMufflerPayload.STREAM_CODEC, ParticleMufflerNetworking::receiveUpdateFilteredMuffler);
    }

    private static void receiveUpdateFilteredMuffler(UpdateFilteredMufflerPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                return;
            }

            if (player.level().getBlockEntity(pos) instanceof FilteredParticleMufflerBlockEntity blockEntity) {
                blockEntity.setFilterMode(payload.filterMode());
                blockEntity.setParticleIds(payload.particleIds());
            }
        });
    }

    public record UpdateFilteredMufflerPayload(BlockPos pos, FilterMode filterMode, Set<ResourceLocation> particleIds) implements CustomPacketPayload {
        public static final Type<UpdateFilteredMufflerPayload> TYPE = new Type<>(UPDATE_FILTERED_MUFFLER_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateFilteredMufflerPayload> STREAM_CODEC = StreamCodec.ofMember(UpdateFilteredMufflerPayload::write, UpdateFilteredMufflerPayload::read);

        private static UpdateFilteredMufflerPayload read(RegistryFriendlyByteBuf buffer) {
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

            return new UpdateFilteredMufflerPayload(pos, filterMode, Set.copyOf(particleIds));
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeUtf(filterMode.name());
            buffer.writeVarInt(particleIds.size());
            for (ResourceLocation particleId : particleIds) {
                buffer.writeResourceLocation(particleId);
            }
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
