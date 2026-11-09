package dev.protonblade;

/** Whole EU accounting; exact battery balance is authoritative, durability is a projection. */
public final class BatteryMath {
    public static long clamp(long energy, long capacity) { return Math.max(0, Math.min(energy, capacity)); }
    public static long accepted(long energy, long amount, long capacity) {
        return Math.min(Math.max(0, amount), capacity-clamp(energy, capacity));
    }
    public static int damage(long energy, long capacity, int maxDamage) {
        return Math.min(maxDamage-1, (int)((capacity-clamp(energy,capacity))*maxDamage/capacity));
    }
    public static long applyWear(long energy, int delta, long capacity, int maxDamage) {
        // Capacity is fixed at 100M EU; these products fit long, including extreme int deltas.
        long eu=(Math.abs((long)delta)*capacity+maxDamage-1)/maxDamage;
        return clamp(delta>=0 ? energy-eu : energy+eu, capacity);
    }
    private BatteryMath() {}
}
