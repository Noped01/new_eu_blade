package dev.protonblade;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public final class BladeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue STARTUP_EU, STANDBY_EU;
    public static final ModConfigSpec.ConfigValue<String> OFF_SA, ON_SA, OFF_MODEL, ON_MODEL, OFF_TEXTURE, ON_TEXTURE;
    public static final ModConfigSpec.IntValue OFF_COLOR, ON_COLOR;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OFF_SE, ON_SE;
    static {
        var b = new ModConfigSpec.Builder();
        b.comment("SI EU is the only energy store; SI supplies its standard FE capability.",
            "Server-only cost per selected tick and per native blade hit while powered.").push("energy");
        STARTUP_EU = b.defineInRange("startupEU", 1000, 1, 100_000_000);
        STANDBY_EU = b.defineInRange("standbyAndHitEU", 10, 1, 100_000_000);
        b.pop().comment("Existing SA defaults are placeholders. Register future SAs and set their full IDs here.",
            "Dedicated SEs belong to this addon; reference addons are not required.").push("modes");
        OFF_SA = location(b, "offSA", "slashblade:judgement_cut");
        ON_SA = location(b, "onSA", "slashblade:judgement_cut");
        OFF_SE = b.defineListAllowEmpty("offSE", List.of("proton_fluorescence:proton_edge"), () -> "proton_fluorescence:proton_edge",
            v -> v instanceof String s && ResourceLocation.tryParse(s)!=null);
        ON_SE = b.defineListAllowEmpty("onSE", List.of("proton_fluorescence:proton_resonance",
            "proton_fluorescence:proton_edge"),
            () -> "proton_fluorescence:proton_resonance", v -> v instanceof String s && ResourceLocation.tryParse(s)!=null);
        OFF_MODEL = location(b, "offModel", "proton_fluorescence:model/named/proton_blade/proton_blade.obj");
        ON_MODEL = location(b, "onModel", "proton_fluorescence:model/named/proton_blade/proton_blade.obj");
        OFF_TEXTURE = location(b, "offTexture", "proton_fluorescence:model/named/proton_blade/texture.png");
        ON_TEXTURE = location(b, "onTexture", "proton_fluorescence:model/named/proton_blade/texture.png");
        OFF_COLOR = b.defineInRange("offColorRGB", 0xFF0000, 0, 0xFFFFFF);
        ON_COLOR = b.defineInRange("onColorRGB", 0xFF0000, 0, 0xFFFFFF);
        b.pop();
        SPEC = b.build();
    }
    private static ModConfigSpec.ConfigValue<String> location(ModConfigSpec.Builder b, String key, String value) {
        return b.define(key, value, v -> v instanceof String s && ResourceLocation.tryParse(s) != null);
    }
    private BladeConfig() {}
}
