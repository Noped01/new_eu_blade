package dev.protonblade.api;

import dev.protonblade.BladeConfig;
import dev.protonblade.ProtonBlade;
import dev.protonblade.EdgeRules;
import net.minecraft.world.entity.player.Player;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.SpecialEffectsRegistry;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashSet;
import com.miophas.singularity_iteration.common.armory.ArmoryComponents;
import com.miophas.singularity_iteration.common.armory.ArmoryLink;

/** Public integration surface. No client classes; safe to use from a dedicated server addon. */
public final class ProtonBladeApi {
    private static final Map<BladeMode, ResourceLocation> ARTS = new EnumMap<>(BladeMode.class);
    private static final Map<BladeMode, List<ResourceLocation>> EFFECTS = new EnumMap<>(BladeMode.class);
    public static boolean isBlade(ItemStack stack) { return stack.is(ProtonBlade.BLADE.get()); }
    public static BladeMode mode(ItemStack stack) {
        return BladeMode.of(isBlade(stack) && stack.getOrDefault(ProtonBlade.POWERED.get(),false)
            && ProtonBlade.BLADE.get().getEnergy(stack)>0);
    }
    /** Register the SA in SlashArtsRegistry first; call during enqueued common setup. */
    public static void setSlashArt(BladeMode mode, ResourceLocation registeredArt) { ARTS.put(mode, registeredArt); }
    /** Full managed SE list for one mode. Other SEs acquired in gameplay are preserved. */
    public static void setSpecialEffects(BladeMode mode, List<ResourceLocation> registeredEffects) {
        EFFECTS.put(mode, List.copyOf(registeredEffects));
    }
    public static List<ResourceLocation> specialEffects(BladeMode mode) {
        return EFFECTS.getOrDefault(mode, (mode==BladeMode.ON ? BladeConfig.ON_SE : BladeConfig.OFF_SE)
            .get().stream().map(ResourceLocation::parse).toList());
    }
    /** Use this predicate in future SE handlers to enforce item, on/off state, stored SE, EU and XP rules. */
    public static boolean isEffectActive(ItemStack stack, LivingEntity user, ResourceLocation effect) {
        return isBlade(stack) && (effect.equals(ProtonBlade.id("proton_edge"))
                ? edgeRules(stack,user).swordWave() : mode(stack)==BladeMode.ON)
            && ProtonBlade.BLADE.get().getEnergy(stack)>0
            && SpecialEffectsRegistry.REGISTRY.containsKey(effect)
            && BladeStateAccess.of(stack).map(s -> s.hasSpecialEffect(effect)).orElse(false)
            && SpecialEffect.isEffective(effect,user);
    }
    public static EdgeRules edgeRules(ItemStack stack,LivingEntity user) {
        return EdgeRules.of(user instanceof Player player ? player.experienceLevel : 0,mode(stack)==BladeMode.ON);
    }
    public static ResourceLocation slashArt(BladeMode mode) {
        var id = ARTS.getOrDefault(mode, ResourceLocation.parse((mode==BladeMode.ON ? BladeConfig.ON_SA : BladeConfig.OFF_SA).get()));
        return SlashArtsRegistry.REGISTRY.containsKey(id) ? id : ResourceLocation.parse("slashblade:judgement_cut");
    }
    /** Server-only, free state change for integrations. Normal play uses SI's toggleActive interface. */
    public static void setPowered(ItemStack stack, boolean on) {
        if (!isBlade(stack)) throw new IllegalArgumentException("Not a Proton Fluorescence blade");
        var before=BladeMode.of(stack.getOrDefault(ProtonBlade.POWERED.get(),false));
        var after=BladeMode.of(on && ProtonBlade.BLADE.get().getEnergy(stack)>0);
        stack.set(ProtonBlade.POWERED.get(),after==BladeMode.ON);
        applyProfile(stack);
        if(before!=after) changed(stack,before,after);
    }
    public static void applyProfile(ItemStack stack) {
        if (!isBlade(stack)) return;
        // Also migrate stacks crafted before automatic Armory linking was added.
        if(!stack.has(ArmoryComponents.LINK.get())) stack.set(ArmoryComponents.LINK.get(),new ArmoryLink("mainhand"));
        boolean on=mode(stack)==BladeMode.ON;
        var art=slashArt(BladeMode.of(on));
        var model=ResourceLocation.parse((on ? BladeConfig.ON_MODEL : BladeConfig.OFF_MODEL).get());
        var texture=ResourceLocation.parse((on ? BladeConfig.ON_TEXTURE : BladeConfig.OFF_TEXTURE).get());
        int color=(on ? BladeConfig.ON_COLOR : BladeConfig.OFF_COLOR).get();
        var desired=specialEffects(BladeMode.of(on));
        String fingerprint=on+"|"+art+"|"+model+"|"+texture+"|"+color+"|"+desired;
        if (fingerprint.equals(stack.get(ProtonBlade.APPLIED_PROFILE.get()))) return;
        BladeStateAccess.of(stack).ifPresent(state -> {
            state.setTranslationKey("item.proton_fluorescence.proton_blade");
            state.setSlashArtsKey(art);
            state.setModel(model);
            state.setTexture(texture);
            state.setColorCode(color);
            // Remove only effects managed by our profiles, including a previous config's managed set.
            var managed=new LinkedHashSet<ResourceLocation>();
            managed.addAll(specialEffects(BladeMode.OFF));
            managed.addAll(specialEffects(BladeMode.ON));
            String old=stack.get(ProtonBlade.APPLIED_PROFILE.get());
            if(old!=null && old.contains("|[")) {
                String list=old.substring(old.lastIndexOf("|[")+2,old.length()-1);
                if(!list.isBlank()) for(String id:list.split(", ")) {
                    var parsed=ResourceLocation.tryParse(id);
                    if(parsed!=null) managed.add(parsed);
                }
            }
            managed.forEach(state::removeSpecialEffect);
            desired.stream().filter(SpecialEffectsRegistry.REGISTRY::containsKey).forEach(state::addSpecialEffect);
            stack.set(ProtonBlade.APPLIED_PROFILE.get(),fingerprint);
        });
    }
    public static void changed(ItemStack stack, BladeMode before, BladeMode after) {
        NeoForge.EVENT_BUS.post(new BladeModeEvents.Change(stack,before,after));
    }
    private ProtonBladeApi() {}
}
