package dev.protonblade.api;

public enum BladeMode {
    OFF, ON;
    public static BladeMode of(boolean powered) { return powered ? ON : OFF; }
}
