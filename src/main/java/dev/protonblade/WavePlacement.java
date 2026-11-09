package dev.protonblade;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Start just outside the native 0.3-block projectile hitbox, so the first ray enters it. */
public final class WavePlacement {
    public static Vec3 nearTarget(AABB target,Vec3 motion) {
        var box=target.inflate((double)0.3f);
        var center=box.getCenter();
        var dir=motion.normalize();
        if(dir.lengthSqr()<1.0e-10) dir=new Vec3(0,0,1);
        double entry=Double.POSITIVE_INFINITY;
        if(Math.abs(dir.x)>1.0e-10) entry=Math.min(entry,box.getXsize()*0.5/Math.abs(dir.x));
        if(Math.abs(dir.y)>1.0e-10) entry=Math.min(entry,box.getYsize()*0.5/Math.abs(dir.y));
        if(Math.abs(dir.z)>1.0e-10) entry=Math.min(entry,box.getZsize()*0.5/Math.abs(dir.z));
        double gap=Math.min(0.05,Math.max(0.00001,motion.length()*0.1));
        return center.subtract(dir.scale(entry+gap));
    }
    private WavePlacement() {}
}
