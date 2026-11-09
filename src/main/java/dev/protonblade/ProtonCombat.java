package dev.protonblade;

import dev.protonblade.api.ProtonBladeApi;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** An additional damage instance, using the same bypass tags as SI's tachyon gun. */
public final class ProtonCombat {
    public static final ResourceKey<DamageType> TRUE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,ProtonBlade.id("proton_true"));
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void afterBladeDamage(LivingDamageEvent.Post event) {
        var victim=event.getEntity();
        var source=event.getSource();
        if(victim.level().isClientSide() || APPLYING.get() || source.is(TRUE_DAMAGE) || !victim.isAlive()) return;
        if(!(source.getEntity() instanceof LivingEntity user) || user==victim) return;
        var direct=source.getDirectEntity();
        var blade=ProjectileOrigin.resolve(direct,user);
        if(!ProtonBladeApi.isEffectActive(blade,user,ProtonBlade.id("proton_edge"))) return;
        boolean swordHit=direct instanceof Projectile || (direct==user &&
            (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)));
        if(!swordHit) return;
        float extra=CombatMath.extraTrueDamage(event.getOriginalDamage(),event.getBlockedDamage(),
            ProtonBladeApi.edgeRules(blade,user).extraTrueDamageRatio());
        if(extra<=0) return;
        var type=victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TRUE_DAMAGE);
        var extraSource=new DamageSource(type,direct,user);
        int previousInvulnerability=victim.invulnerableTime;
        APPLYING.set(true);
        try {
            // Like SI's strike(), clear hit cooldown so the second portion is accepted.
            // Armor/effect bypass is declared by damage-type tags; never write a victim's health directly.
            victim.invulnerableTime=0;
            victim.hurt(extraSource,extra);
        } finally {
            victim.invulnerableTime=Math.max(previousInvulnerability,victim.invulnerableTime);
            APPLYING.remove();
        }
    }
    private ProtonCombat() {}
}
