package dev.protonblade;

import com.miophas.singularity_iteration.common.armory.ArmoryComponents;
import com.miophas.singularity_iteration.common.armory.ArmoryLink;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;
import com.miophas.singularity_iteration.core.api.item.IElectricToolItem;
import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import com.miophas.singularity_iteration.core.api.tool.IToolModeProvider;
import com.miophas.singularity_iteration.core.runtime.energy.EnergyPackCharging;
import dev.protonblade.api.BladeMode;
import dev.protonblade.api.ProtonBladeApi;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateData;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeDataComponents;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Native Resharpened item, implementing SI's battery and quantum-sword mode interfaces. */
public final class ProtonBladeItem extends ItemSlashBlade implements IElectricToolItem, IToolModeProvider {
    public static final long CAPACITY=100_000_000L, TRANSFER=32768;
    public static final int DURABILITY=1280, TIER=5;
    public ProtonBladeItem() { super(Tiers.DIAMOND,1024,-2.4f,properties()); }
    private static Properties properties() {
        var data=new BladeStateData("item.proton_fluorescence.proton_blade",1024,0,0,0,false,false,
            ResourceLocation.parse("slashblade:judgement_cut"),true,Optional.empty(),false,
            ResourceLocation.parse("slashblade:standby"),CarryType.KATANA,0xFF0000,false,Vec3.ZERO,
            Optional.of(ProtonBlade.id("model/named/proton_blade/texture.png")),
            Optional.of(ProtonBlade.id("model/named/proton_blade/proton_blade.obj")),List.of());
        return new Properties().durability(DURABILITY).setNoRepair().rarity(Rarity.EPIC)
            .component(SlashBladeDataComponents.BLADE_STATE_DATA.get(),data)
            .component(mio_icif_data_components.BATTERY_ENERGY.get(),new BatteryEnergy(CAPACITY,CAPACITY))
            .component(ProtonBlade.PROJECTED_DAMAGE.get(),0)
            .component(ProtonBlade.POWERED.get(),false)
            .component(ArmoryComponents.LINK.get(),new ArmoryLink("mainhand"));
    }
    @Override public long getMaxEnergy() { return CAPACITY; }
    @Override public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED);
    }
    @Override public long getEnergy(ItemStack stack) {
        var battery=stack.get(mio_icif_data_components.BATTERY_ENERGY.get());
        long energy=BatteryMath.clamp(battery==null ? 0 : battery.energy(),CAPACITY);
        int projected=stack.getOrDefault(ProtonBlade.PROJECTED_DAMAGE.get(),stack.getDamageValue());
        long delta=(long)stack.getDamageValue()-projected;
        return delta==0 ? energy : BatteryMath.applyWear(energy,(int)Math.clamp(delta,Integer.MIN_VALUE,Integer.MAX_VALUE),CAPACITY,stack.getMaxDamage());
    }
    @Override public void setEnergy(ItemStack stack,long energy) {
        long value=BatteryMath.clamp(energy,CAPACITY);
        int damage=BatteryMath.damage(value,CAPACITY,stack.getMaxDamage());
        stack.set(mio_icif_data_components.BATTERY_ENERGY.get(),new BatteryEnergy(value,CAPACITY));
        // Direct projection: never feed a rounding change back into wear accounting.
        stack.set(DataComponents.DAMAGE,damage);
        stack.set(ProtonBlade.PROJECTED_DAMAGE.get(),damage);
        BladeStateAccess.of(stack).ifPresent(state -> {
            if(value==0) state.setBroken(true);
            else if(damage==0 && !state.isSealed()) state.setBroken(false);
        });
    }
    public void reconcile(ItemStack stack) { setEnergy(stack,getEnergy(stack)); }
    @Override public long addEnergy(ItemStack stack,long amount) {
        long before=getEnergy(stack), accepted=BatteryMath.accepted(before,amount,CAPACITY);
        if(accepted>0) setEnergy(stack,before+accepted);
        return accepted;
    }
    @Override public long extractEnergy(ItemStack stack,long amount) {
        long before=getEnergy(stack), removed=Math.min(before,Math.max(0,amount));
        if(removed>0) setEnergy(stack,before-removed);
        return removed;
    }
    @Override public boolean isFull(ItemStack stack) { return getEnergy(stack)==CAPACITY; }
    @Override public boolean isEmpty(ItemStack stack) { return getEnergy(stack)==0; }
    @Override public long getChargeRate(ItemStack stack) { return TRANSFER; }
    @Override public long getEnergyPerUse() { return (CAPACITY+DURABILITY-1)/DURABILITY; }
    @Override public int getToolTier() { return TIER; }
    @Override public void setDamage(ItemStack stack,int damage) {
        super.setDamage(stack,damage);
        reconcile(stack);
    }
    @Override public <T extends LivingEntity> int damageItem(ItemStack stack,int amount,T entity,Consumer<Item> onBroken) {
        if(entity.level().isClientSide()) return 0;
        reconcile(stack);
        boolean exhausted=amount>0 && (long)stack.getDamageValue()+amount>=stack.getMaxDamage();
        int remaining=super.damageItem(stack,amount,entity,onBroken);
        if(exhausted && !stack.isEmpty()) { setEnergy(stack,0); return 0; }
        return remaining;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        if(!level.isClientSide()) {
            reconcile(stack);
            if(stack.getOrDefault(ProtonBlade.POWERED.get(),false) && isEmpty(stack))
                ProtonBladeApi.setPowered(stack,false);
            EnergyPackCharging.trackHolder(stack,entity);
            if(entity instanceof LivingEntity holder) EnergyPackCharging.chargeFromArmor(stack,holder);
            ProjectileOrigin.identity(stack);
            ProtonBladeApi.applyProfile(stack);
        }
        super.inventoryTick(stack,level,entity,slot,selected);
        if(!level.isClientSide()) reconcile(stack);
    }
    @Override public void toggleActive(ItemStack stack,Player player) {
        if(player.level().isClientSide() || stack.getItem()!=this) return;
        boolean requested=ProtonBladeApi.mode(stack)!=BladeMode.ON;
        if(requested) {
            EnergyPackCharging.chargeFromArmor(stack,player);
            if(!BatteryTransfer.consume(stack,this,BladeConfig.STARTUP_EU.get())) {
                player.displayClientMessage(Component.translatable("message.proton_fluorescence.no_energy"),true);
                return;
            }
        }
        ProtonBladeApi.setPowered(stack,requested);
        player.displayClientMessage(Component.translatable(requested ? "tooltip.proton_fluorescence.on" : "tooltip.proton_fluorescence.off"),true);
    }
    @Override public List<Component> toolModes(ItemStack stack) {
        return List.of(Component.translatable("tooltip.proton_fluorescence.off"),Component.translatable("tooltip.proton_fluorescence.on"));
    }
    @Override public int toolModeIndex(ItemStack stack) { return ProtonBladeApi.mode(stack)==BladeMode.ON ? 1 : 0; }
    @Override public void selectToolMode(ItemStack stack,Player player,int index) {
        if(index>=0 && index<2 && index!=toolModeIndex(stack)) toggleActive(stack,player);
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flags) {
        super.appendHoverText(stack,context,lines,flags);
        lines.add(Component.translatable("tooltip.proton_fluorescence.energy",getEnergy(stack),CAPACITY,TRANSFER,TIER));
    }
}
