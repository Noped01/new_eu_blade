package dev.protonblade;

import java.util.Random;

/** Policy boundaries and conservation, plus verification against unmodified installed APIs. No world is started. */
public final class RuntimeSmoke {
    private static int assertions;
    private static void check(boolean condition,String message) {
        assertions++;
        if(!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        for(int level:new int[]{0,34,35,36,100}) for(boolean on:new boolean[]{false,true}) {
            var rules=EdgeRules.of(level,on);
            boolean high=level>=35;
            check(rules.swordWave()==(high||on),"sword wave: level="+level+", powered="+on);
            check(rules.teleportToLock()==(high&&on),"teleport gate");
            check(rules.extraTrueDamageRatio()==(high&&on ? 1.0 : high||on ? 0.5 : 0.0),"true damage tier");
        }
        long capacity=ProtonBladeItem.CAPACITY;
        int durability=ProtonBladeItem.DURABILITY;
        check(BatteryMath.damage(0,capacity,durability)==durability-1,"zero EU never destroys the item");
        check(BatteryMath.damage(capacity,capacity,durability)==0,"fully charged");
        check(BatteryMath.accepted(capacity,1,capacity)==0,"reject overcharge");
        check(BatteryMath.accepted(capacity-1,Long.MAX_VALUE,capacity)==1,"saturate huge packet");
        check(BatteryMath.accepted(10,-1,capacity)==0,"reject negative packet");
        check(BatteryMath.applyWear(capacity,Integer.MAX_VALUE,capacity,durability)==0,"large wear");
        check(BatteryMath.applyWear(0,Integer.MIN_VALUE,capacity,durability)==capacity,"large repair");
        var random=new Random(90411);
        for(int i=0;i<256;i++) {
            long stored=random.nextLong(capacity+1);
            long packet=random.nextLong(Long.MAX_VALUE);
            long added=BatteryMath.accepted(stored,packet,capacity);
            check(added>=0 && added<=packet && stored+added<=capacity,"battery conservation");
            check(BatteryMath.applyWear(stored,0,capacity,durability)==stored,"projection never loses small EU packets");
            check(BatteryMath.applyWear(stored,1,capacity,durability)<=stored,"wear never adds EU");
        }
        check(CombatMath.extraTrueDamage(100,0,0.5)==50,"50% is additional");
        check(CombatMath.extraTrueDamage(100,0,1.0)==100,"100% is additional, including wave hits");
        check(CombatMath.extraTrueDamage(100,100,1.0)==0,"fully shield-blocked hit");
        check(CombatMath.extraTrueDamage(100,40,0.5)==30,"partial shield block");
        check(CombatMath.extraTrueDamage(Float.NaN,0,1)==0,"NaN");
        check(CombatMath.extraTrueDamage(Float.POSITIVE_INFINITY,0,1)==0,"infinite damage");
        // Use Minecraft's actual AABB.clip, the same primitive used by native Drive collision.
        // Starting inside a target is not sufficient: this overload only returns an entering face.
        var hitbox=new net.minecraft.world.phys.AABB(-0.3,0,-0.3,0.3,1.8,0.3);
        var padded=hitbox.inflate((double)0.3f);
        var center=padded.getCenter();
        check(padded.clip(center,center.add(0,0,1.5)).isEmpty(),"center-spawn collision regression");
        for(var box:new net.minecraft.world.phys.AABB[]{hitbox,new net.minecraft.world.phys.AABB(-8,-4,-8,8,4,8),
                new net.minecraft.world.phys.AABB(90,100,-60,90.1,100.1,-59.9)}) {
            for(var motion:new net.minecraft.world.phys.Vec3[]{new net.minecraft.world.phys.Vec3(0,0,1.5),
                    new net.minecraft.world.phys.Vec3(0,0,-1.5),new net.minecraft.world.phys.Vec3(1.5,0,0),
                    new net.minecraft.world.phys.Vec3(0,-1.5,0),new net.minecraft.world.phys.Vec3(0.8,0.9,-0.9),
                    new net.minecraft.world.phys.Vec3(0,0,0.15),new net.minecraft.world.phys.Vec3(0.08,0.09,-0.09)}) {
                var start=WavePlacement.nearTarget(box,motion);
                check(!box.inflate((double)0.3f).contains(start),"spawn outside expanded target");
                check(box.inflate((double)0.3f).clip(start,start.add(motion)).isPresent(),"first-step native ray intersects target");
            }
        }
        String[] classes={"ProtonBlade","ProtonBladeItem","ProtonElectricCapability","BladeRuntime",
            "ProjectileOrigin","ProtonCombat","api.ProtonBladeApi","api.BladeModeEvents$Change",
            "api.BladeModeEvents$Tick","api.BladeModeEvents$Hit","client.BladeClient"};
        for(String name:classes) {
            var cls=Class.forName("dev.protonblade."+name,false,RuntimeSmoke.class.getClassLoader());
            check(cls.getDeclaredMethods().length>0,"verify "+name);
            cls.getDeclaredConstructors();
        }
        check(ProtonBladeItem.class.getSuperclass()==mods.flammpfeil.slashblade.item.ItemSlashBlade.class,"stock item base");
        check(com.miophas.singularity_iteration.core.api.item.IElectricToolItem.class.isAssignableFrom(ProtonBladeItem.class),"SI equipment charging API");
        check(com.miophas.singularity_iteration.core.api.tool.IToolModeProvider.class.isAssignableFrom(ProtonBladeItem.class),"quantum sword mode API");
        for(String name:new String[]{"cn.mmf.energyblade.Energyblade","org.scex.sislashblade.SISlashBlade",
            "cn.mmf.slashblade_addon.SlashbladeAddon","org.scex.slashbladelegacy.BladeStateConstructorPatch"}) {
            try { Class.forName(name,false,RuntimeSmoke.class.getClassLoader());throw new AssertionError("Reference addon on test classpath: "+name); }
            catch(ClassNotFoundException expected) { check(true,"reference-only dependency absent"); }
        }
        var link=new com.miophas.singularity_iteration.common.armory.ArmoryLink("mainhand");
        check(link.pieceOrDefault().slot==net.minecraft.world.entity.EquipmentSlot.MAINHAND,"SI armory mainhand mapping");
        System.out.println("PASS: "+assertions+" assertions; stock API class verification. Not a world startup/gameplay test.");
    }
}
