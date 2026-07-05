package dev.liquidcatmofu.particlemuffler.mixin;

import dev.liquidcatmofu.particlemuffler.client.ParticleMufflerClientRegistry;
import dev.liquidcatmofu.particlemuffler.client.ParticleInspection;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ParticleEngine.class, priority = 1100)
public abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, order = 1100)
    private void particlemuffler$suppressParticle(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfoReturnable<Particle> cir
    ) {
        if (ParticleMufflerClientRegistry.isUnfilteredSuppressedFast(x, y, z)) {
            cir.setReturnValue(null);
            return;
        }

        if (ParticleMufflerClientRegistry.hasAnyFilteredMuffler()
                && ParticleMufflerClientRegistry.isFilteredSuppressedFast(BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()), x, y, z)) {
            cir.setReturnValue(null);
            return;
        }

        if (ParticleInspection.isActive()) {
            ParticleInspection.record(BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()));
        }
    }
}
