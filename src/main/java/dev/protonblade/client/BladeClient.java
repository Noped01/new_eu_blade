package dev.protonblade.client;

import mods.flammpfeil.slashblade.client.renderer.SlashBladeTEISR;
import dev.protonblade.ProtonBlade;
import dev.protonblade.BladeConfig;
import dev.protonblade.api.BladeMode;
import dev.protonblade.api.ProtonBladeApi;
import mods.flammpfeil.slashblade.client.ClientHandler;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid=ProtonBlade.ID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class BladeClient {
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent e) {
        e.registerItem(new IClientItemExtensions() {
            private final SlashBladeTEISR renderer=new SlashBladeTEISR(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, ProtonBlade.BLADE.get());
    }
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult e) { ClientHandler.bakeBlade(ProtonBlade.BLADE.get(),e); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> ItemProperties.register(ProtonBlade.BLADE.get(),ResourceLocation.parse("slashblade:user"),
            (stack,level,entity,seed) -> { BladeModel.user=entity; return 0; }));
    }
    @EventBusSubscriber(modid=ProtonBlade.ID, value=Dist.CLIENT)
    public static final class Tooltip {
        @SubscribeEvent public static void tooltip(ItemTooltipEvent e) {
            if(!ProtonBladeApi.isBlade(e.getItemStack())) return;
            // A fresh /give stack has the upstream default translation until its first inventory tick.
            if(!e.getItemStack().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
                e.getToolTip().set(0,Component.translatable("item.proton_fluorescence.proton_blade").withStyle(ChatFormatting.RED));
            boolean on=ProtonBladeApi.mode(e.getItemStack())==BladeMode.ON;
            e.getToolTip().add(Component.translatable(on ? "tooltip.proton_fluorescence.on" : "tooltip.proton_fluorescence.off").withStyle(on ? ChatFormatting.RED : ChatFormatting.GRAY));
            e.getToolTip().add(Component.translatable("tooltip.proton_fluorescence.switch").withStyle(ChatFormatting.DARK_GRAY));
            e.getToolTip().add(Component.translatable("tooltip.proton_fluorescence.se_low").withStyle(ChatFormatting.DARK_RED));
            e.getToolTip().add(Component.translatable("tooltip.proton_fluorescence.se_high").withStyle(ChatFormatting.DARK_RED));
        }
    }
    private BladeClient() {}
}
