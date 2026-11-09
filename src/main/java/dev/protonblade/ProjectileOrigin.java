package dev.protonblade;

import dev.protonblade.api.ProtonBladeApi;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import java.util.UUID;

/** Capture once at spawn. Unknown/reloaded origins never fall back to the current main hand. */
public final class ProjectileOrigin {
    private static final String TAG="proton_fluorescence:origin";
    public static String identity(ItemStack stack) {
        if(!ProtonBladeApi.isBlade(stack)) return "";
        String value=stack.get(ProtonBlade.BLADE_ID.get());
        if(value==null || value.isBlank()) {
            value=UUID.randomUUID().toString();
            stack.set(ProtonBlade.BLADE_ID.get(),value);
        }
        return value;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide() || !(event.getEntity() instanceof Projectile projectile)) return;
        if(projectile.getPersistentData().contains(TAG)) return;
        var origin=new net.minecraft.nbt.CompoundTag();
        // Remember unknown sources too. A later weapon switch must never capture them retroactively.
        if(!event.loadedFromDisk() && projectile.getOwner() instanceof LivingEntity owner) {
            var blade=owner.getMainHandItem();
            if(ProtonBladeApi.isBlade(blade)) {
                origin.putString("blade",identity(blade));
                origin.putUUID("owner",owner.getUUID());
            }
        }
        projectile.getPersistentData().put(TAG,origin);
    }
    public static void bind(Projectile projectile,LivingEntity owner,ItemStack blade) {
        if(projectile==null || projectile.level().isClientSide() || !ProtonBladeApi.isBlade(blade)) return;
        var origin=new net.minecraft.nbt.CompoundTag();
        origin.putString("blade",identity(blade));
        origin.putUUID("owner",owner.getUUID());
        projectile.getPersistentData().put(TAG,origin);
    }
    private static boolean matches(ItemStack stack,String id) {
        return ProtonBladeApi.isBlade(stack) && id.equals(stack.get(ProtonBlade.BLADE_ID.get()));
    }
    private static ItemStack carried(LivingEntity holder,String id) {
        if(holder instanceof Player player) {
            for(int i=0;i<player.getInventory().getContainerSize();i++) {
                var stack=player.getInventory().getItem(i);
                if(matches(stack,id)) return stack;
            }
        }
        for(var stack:holder.getAllSlots()) if(matches(stack,id)) return stack;
        return ItemStack.EMPTY;
    }
    public static ItemStack resolve(Entity direct,LivingEntity attacker) {
        if(!(direct instanceof Projectile projectile)) return attacker.getMainHandItem();
        var origin=projectile.getPersistentData().getCompound(TAG);
        if(!origin.hasUUID("owner") || !origin.getUUID("owner").equals(attacker.getUUID())) return ItemStack.EMPTY;
        String id=origin.getString("blade");
        if(id.isBlank()) return ItemStack.EMPTY;
        var local=carried(attacker,id);
        if(!local.isEmpty()) return local;
        MinecraftServer server=attacker.getServer();
        if(server==null) return ItemStack.EMPTY;
        for(var player:server.getPlayerList().getPlayers()) {
            var stack=carried(player,id);
            if(!stack.isEmpty()) return stack;
        }
        for(var level:server.getAllLevels()) for(var entity:level.getAllEntities()) {
            if(entity instanceof ItemEntity item && matches(item.getItem(),id)) return item.getItem();
        }
        // An unloaded/container-held or destroyed source cannot grant an effect to a replacement blade.
        return ItemStack.EMPTY;
    }
    private ProjectileOrigin() {}
}
