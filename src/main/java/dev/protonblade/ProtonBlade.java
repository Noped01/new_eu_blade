package dev.protonblade;

import com.mojang.serialization.Codec;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;

@Mod(ProtonBlade.ID)
public final class ProtonBlade {
    public static final String ID = "proton_fluorescence";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    public static final DeferredHolder<Item, ProtonBladeItem> BLADE = ITEMS.register("proton_blade", ProtonBladeItem::new);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> APPLIED_PROFILE =
        COMPONENTS.register("applied_profile", () -> DataComponentType.<String>builder()
            .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> POWERED =
        COMPONENTS.register("powered", () -> DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PROJECTED_DAMAGE =
        COMPONENTS.register("projected_damage", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLADE_ID =
        COMPONENTS.register("blade_id", () -> DataComponentType.<String>builder()
            .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredRegister<SpecialEffect> EFFECTS = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, ID);
    public static final DeferredHolder<SpecialEffect, SpecialEffect> PROTON_RESONANCE =
        EFFECTS.register("proton_resonance", () -> new SpecialEffect(0, false, false));
    public static final DeferredHolder<SpecialEffect, SpecialEffect> PROTON_EDGE =
        EFFECTS.register("proton_edge", () -> new SpecialEffect(0, false, false));

    public ProtonBlade(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        EFFECTS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, BladeConfig.SPEC);
        bus.addListener(this::capabilities);
        NeoForge.EVENT_BUS.register(BladeRuntime.class);
        NeoForge.EVENT_BUS.register(ProtonCombat.class);
        NeoForge.EVENT_BUS.register(ProjectileOrigin.class);
    }

    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(IMioIcifCapabilities.ELECTRIC_ITEM,
            (stack, context) -> new ProtonElectricCapability(BLADE.get()), BLADE.get());
        // SI supplies the FE capability for every IBatteryItem. Never register a second supplier.
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
