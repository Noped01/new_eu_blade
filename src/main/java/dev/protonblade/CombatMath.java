package dev.protonblade;

/** Pure combat arithmetic, kept independent from game initialization for repeatable checks. */
public final class CombatMath {
    public static float extraTrueDamage(float original, float blocked, double ratio) {
        if(!Float.isFinite(original) || !Float.isFinite(blocked) || !Double.isFinite(ratio) || ratio<=0) return 0;
        double base=Math.max(0, (double)original-Math.max(0,blocked));
        return (float)Math.min(Float.MAX_VALUE, base*ratio);
    }
    private CombatMath() {}
}
