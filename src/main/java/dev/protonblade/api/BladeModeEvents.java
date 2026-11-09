package dev.protonblade.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/** Register handlers on NeoForge.EVENT_BUS. All three hooks are server-side only. */
public final class BladeModeEvents {
    public static final class Change extends Event {
        private final ItemStack blade;
        private final BladeMode before, after;
        public Change(ItemStack blade, BladeMode before, BladeMode after) { this.blade=blade; this.before=before; this.after=after; }
        public ItemStack blade() { return blade; }
        public BladeMode before() { return before; }
        public BladeMode after() { return after; }
    }
    public static final class Tick extends Event {
        private final ItemStack blade;
        private final LivingEntity holder;
        private final BladeMode mode;
        private final boolean selected;
        public Tick(ItemStack blade, LivingEntity holder, BladeMode mode, boolean selected) {
            this.blade=blade; this.holder=holder; this.mode=mode; this.selected=selected;
        }
        public ItemStack blade() { return blade; }
        public LivingEntity holder() { return holder; }
        public BladeMode mode() { return mode; }
        public boolean selected() { return selected; }
    }
    public static final class Hit extends Event {
        private final ItemStack blade;
        private final LivingEntity user, target;
        private final BladeMode mode;
        public Hit(ItemStack blade, LivingEntity user, LivingEntity target, BladeMode mode) {
            this.blade=blade; this.user=user; this.target=target; this.mode=mode;
        }
        public ItemStack blade() { return blade; }
        public LivingEntity user() { return user; }
        public LivingEntity target() { return target; }
        public BladeMode mode() { return mode; }
    }
    private BladeModeEvents() {}
}
