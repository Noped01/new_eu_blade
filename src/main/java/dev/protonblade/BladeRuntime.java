package dev.protonblade;

import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import dev.protonblade.api.BladeMode;
import dev.protonblade.api.BladeModeEvents;
import dev.protonblade.api.ProtonBladeApi;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.slasharts.Drive;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class BladeRuntime {
    private static final float VOID_WAVE_SPEED=0.15f;
    private static final int VOID_WAVE_LIFETIME=10; // 0.5 seconds at 20 TPS.
    private static final ThreadLocal<Boolean> EMITTING=ThreadLocal.withInitial(() -> false);
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void updated(SlashBladeEvent.UpdateEvent e) {
        if(e.getLevel().isClientSide() || !ProtonBladeApi.isBlade(e.getBlade())) return;
        var blade=e.getBlade();
        if(blade.getOrDefault(ProtonBlade.POWERED.get(),false) &&
            (ProtonBlade.BLADE.get().isEmpty(blade) || e.isSelected() &&
                !BatteryTransfer.consume(blade,ProtonBlade.BLADE.get(),BladeConfig.STANDBY_EU.get())))
            ProtonBladeApi.setPowered(blade,false);
        ProtonBladeApi.applyProfile(blade);
        if(e.getEntity() instanceof LivingEntity holder) {
            if(e.isSelected()) pinRank(holder,blade);
            NeoForge.EVENT_BUS.post(new BladeModeEvents.Tick(blade,holder,ProtonBladeApi.mode(blade),e.isSelected()));
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(SlashBladeEvent.HitEvent e) {
        if(e.getUser().level().isClientSide() || !ProtonBladeApi.isBlade(e.getBlade())) return;
        var blade=e.getBlade();
        if(ProtonBladeApi.mode(blade)==BladeMode.ON &&
            !BatteryTransfer.consume(blade,ProtonBlade.BLADE.get(),BladeConfig.STANDBY_EU.get()))
            ProtonBladeApi.setPowered(blade,false);
        pinRank(e.getUser(),blade);
        NeoForge.EVENT_BUS.post(new BladeModeEvents.Hit(blade,e.getUser(),e.getTarget(),ProtonBladeApi.mode(blade)));
    }
    @SubscribeEvent
    public static void powered(SlashBladeEvent.PowerBladeEvent e) {
        if(ProtonBladeApi.isEffectActive(e.getBlade(),e.getUser(),ProtonBlade.id("proton_resonance"))) e.setPowered(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void slash(SlashBladeEvent.DoSlashEvent e) {
        if(e.getUser().level().isClientSide() || EMITTING.get() || !(e.getUser() instanceof Player)) return;
        if(!ProtonBladeApi.isEffectActive(e.getBlade(),e.getUser(),ProtonBlade.id("proton_edge"))) return;
        EMITTING.set(true);
        try {
            // Independent SE, using Resharpened's native Drive API. Reference: SJAP BurstDrive behavior.
            var drive=Drive.doSlash(e.getUser(),e.getRoll(),e.getYRot(),10,
                e.getSlashBladeState().getColorCode(),Vec3.ZERO,false,e.getDamage(),null,1.5f);
            ProjectileOrigin.bind(drive,e.getUser(),e.getBlade());
            if(drive!=null && drive.isAddedToLevel() && ProtonBladeApi.edgeRules(e.getBlade(),e.getUser()).teleportToLock()) {
                var target=e.getSlashBladeState().getTargetEntity(e.getUser().level());
                if(target!=null && target.isAlive() && target!=e.getUser() && !e.getUser().isAlliedTo(target)) {
                    // setSpeed stores the launch parameter; update velocity too because doSlash already shot it.
                    drive.setSpeed(VOID_WAVE_SPEED);
                    drive.setDeltaMovement(drive.getDeltaMovement().normalize().scale(VOID_WAVE_SPEED));
                    drive.setLifetime(VOID_WAVE_LIFETIME);
                    var start=WavePlacement.nearTarget(target.getBoundingBox(),drive.getDeltaMovement());
                    drive.setPos(start.x,start.y,start.z);
                    // Relocate the original wave; retain native collision and hit processing.
                    // Never replace an area-capable attack with a one-target forced-hit call.
                }
            }
        } finally { EMITTING.remove(); }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void afterDamage(LivingDamageEvent.Post e) {
        pinRank(e.getEntity(),e.getEntity().getMainHandItem());
    }
    public static void pinRank(LivingEntity holder,ItemStack blade) {
        if(holder.level().isClientSide() || !ProtonBladeApi.isEffectActive(blade,holder,ProtonBlade.id("proton_resonance"))) return;
        var rank=holder.getData(CapabilityConcentrationRank.RANK_POINT.get());
        long missing=rank.getMaxCapacity()-rank.getRankPoint(holder.level().getGameTime());
        if(missing>0) rank.addRankPoint(holder,missing);
    }
    private BladeRuntime() {}
}
