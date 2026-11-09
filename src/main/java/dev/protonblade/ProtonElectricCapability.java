package dev.protonblade;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities.ICapabilityElectricItem;
import net.minecraft.world.item.ItemStack;

/** SI integration policy: accept EU, allow internal consumption, never export to another device. */
public final class ProtonElectricCapability implements ICapabilityElectricItem {
    private final ProtonBladeItem blade;
    public ProtonElectricCapability(ProtonBladeItem blade) { this.blade=blade; }
    @Override public long getMaxCharge(ItemStack stack) { return blade.getMaxEnergy(); }
    @Override public long getTransferLimit(ItemStack stack) { return blade.getChargeRate(stack); }
    @Override public int getTier(ItemStack stack) { return blade.getToolTier(); }
    @Override public boolean canProvideEnergy(ItemStack stack) { return false; }
    @Override public long charge(ItemStack stack,long amount,int tier,boolean ignoreLimit,boolean simulate) {
        if(stack.getItem()!=blade || stack.getCount()!=1 || tier>getTier(stack) || amount<=0) return 0;
        long accepted=BatteryMath.accepted(blade.getEnergy(stack),ignoreLimit ? amount : Math.min(amount,getTransferLimit(stack)),getMaxCharge(stack));
        return simulate ? accepted : blade.addEnergy(stack,accepted);
    }
    @Override public long discharge(ItemStack stack,long amount,int tier,boolean ignoreLimit,boolean externally,boolean simulate) {
        if(stack.getItem()!=blade || stack.getCount()!=1 || externally || amount<=0) return 0;
        long removed=Math.min(blade.getEnergy(stack),ignoreLimit ? amount : Math.min(amount,getTransferLimit(stack)));
        return simulate ? removed : blade.extractEnergy(stack,removed);
    }
}
