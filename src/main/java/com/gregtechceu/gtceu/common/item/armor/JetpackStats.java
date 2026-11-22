package com.gregtechceu.gtceu.common.item.armor;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import org.jetbrains.annotations.Nullable;

public record JetpackStats(double sprintEnergyModifier, double sprintSpeedModifier, double verticalHoverSpeed,
                           double verticalHoverSlowSpeed, double verticalAcceleration, double verticalSpeed,
                           double sidewaysSpeed, boolean removeMiningSpeedPenalty,
                           @Nullable ParticleOptions particle, float fallDamageReduction) {

    public static final JetpackStats DEFAULT = new JetpackStats(1.0D, 1.0D, 0.18D, 0.14D, 0.1D, 0.22D, 0.0D, true,
            ParticleTypes.LARGE_SMOKE, 0.0f);

    public static final JetpackStats ELECTRIC = DEFAULT.statBuilder()
            .verticalHoverSlowSpeed(0.1D)
            .verticalAcceleration(0.12D)
            .verticalSpeed(0.3D)
            .sidewaysSpeed(0.08D)
            .particle(ParticleTypes.SMOKE)
            .build();
    public static final JetpackStats ADVANCED = DEFAULT.statBuilder()
            .sprintEnergyModifier(2.5D)
            .sprintSpeedModifier(1.3D)
            .verticalHoverSpeed(0.34D)
            .verticalHoverSlowSpeed(0.03D)
            .verticalAcceleration(0.13D)
            .verticalSpeed(0.48D)
            .sidewaysSpeed(0.14D)
            .particle(ParticleTypes.CLOUD)
            .fallDamageReduction(2.0f)
            .build();
    public static final JetpackStats NANO_SUITE = DEFAULT.statBuilder()
            .sprintEnergyModifier(4.0D)
            .sprintSpeedModifier(1.8D)
            .verticalHoverSpeed(0.4D)
            .verticalHoverSlowSpeed(0.005D)
            .verticalAcceleration(0.14D)
            .verticalSpeed(0.8D)
            .sidewaysSpeed(0.19D)
            .particle(null)
            .fallDamageReduction(3.5f)
            .build();
    public static final JetpackStats QUARK_SUITE = DEFAULT.statBuilder()
            .sprintEnergyModifier(6.0D)
            .sprintSpeedModifier(2.4D)
            .verticalHoverSpeed(0.45D)
            .verticalHoverSlowSpeed(0.0D)
            .verticalAcceleration(0.15D)
            .verticalSpeed(0.9D)
            .sidewaysSpeed(0.21D)
            .particle(null)
            .fallDamageReduction(8.0f)
            .build();

    public static Builder builder() {
        return DEFAULT.statBuilder();
    }

    public Builder statBuilder() {
        return new Builder(this);
    }

    public static final class Builder {

        private double sprintEnergyModifier;
        private double sprintSpeedModifier;
        private double verticalHoverSpeed;
        private double verticalHoverSlowSpeed;
        private double verticalAcceleration;
        private double verticalSpeed;
        private double sidewaysSpeed;
        private boolean removeMiningSpeedPenalty;
        @Nullable
        private ParticleOptions particle;
        private float fallDamageReduction;

        private Builder(JetpackStats base) {
            this.sprintEnergyModifier = base.sprintEnergyModifier;
            this.sprintSpeedModifier = base.sprintSpeedModifier;
            this.verticalHoverSpeed = base.verticalHoverSpeed;
            this.verticalHoverSlowSpeed = base.verticalHoverSlowSpeed;
            this.verticalAcceleration = base.verticalAcceleration;
            this.verticalSpeed = base.verticalSpeed;
            this.sidewaysSpeed = base.sidewaysSpeed;
            this.removeMiningSpeedPenalty = base.removeMiningSpeedPenalty;
            this.particle = base.particle;
            this.fallDamageReduction = base.fallDamageReduction;
        }

        public Builder sprintEnergyModifier(double value) {
            this.sprintEnergyModifier = value;
            return this;
        }

        public Builder sprintSpeedModifier(double value) {
            this.sprintSpeedModifier = value;
            return this;
        }

        public Builder verticalHoverSpeed(double value) {
            this.verticalHoverSpeed = value;
            return this;
        }

        public Builder verticalHoverSlowSpeed(double value) {
            this.verticalHoverSlowSpeed = value;
            return this;
        }

        public Builder verticalAcceleration(double value) {
            this.verticalAcceleration = value;
            return this;
        }

        public Builder verticalSpeed(double value) {
            this.verticalSpeed = value;
            return this;
        }

        public Builder sidewaysSpeed(double value) {
            this.sidewaysSpeed = value;
            return this;
        }

        public Builder removeMiningSpeedPenalty(boolean value) {
            this.removeMiningSpeedPenalty = value;
            return this;
        }

        public Builder particle(@Nullable ParticleOptions value) {
            this.particle = value;
            return this;
        }

        public Builder fallDamageReduction(float value) {
            this.fallDamageReduction = value;
            return this;
        }

        public JetpackStats build() {
            return new JetpackStats(sprintEnergyModifier, sprintSpeedModifier, verticalHoverSpeed,
                    verticalHoverSlowSpeed, verticalAcceleration, verticalSpeed, sidewaysSpeed,
                    removeMiningSpeedPenalty, particle, fallDamageReduction);
        }
    }
}
