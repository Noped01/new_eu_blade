package dev.protonblade;

/** The dedicated SE's four cases. Level 35 belongs to the upper tier. */
public record EdgeRules(boolean swordWave, boolean teleportToLock, double extraTrueDamageRatio) {
    public static EdgeRules of(int level,boolean powered) {
        if(level>=35) return new EdgeRules(true,powered,powered ? 1.0 : 0.5);
        return new EdgeRules(powered,false,powered ? 0.5 : 0.0);
    }
}
