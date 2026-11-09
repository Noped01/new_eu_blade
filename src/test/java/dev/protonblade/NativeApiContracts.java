package dev.protonblade;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarFile;

/** Bytecode API contracts. Dependency classes are inspected, never patched or initialized. */
public final class NativeApiContracts {
    private static final String SB="mods/flammpfeil/slashblade/", SI="com/miophas/singularity_iteration/";
    private static final Map<String,ClassNode> CACHE=new HashMap<>();
    private static int references;
    private static void require(boolean test,String message) { if(!test) throw new AssertionError(message); }
    private static ClassNode read(String name) throws Exception {
        if(CACHE.containsKey(name))return CACHE.get(name);
        try(InputStream input=NativeApiContracts.class.getClassLoader().getResourceAsStream(name+".class")) {
            require(input!=null,"Missing class: "+name);
            var node=new ClassNode();new ClassReader(input).accept(node,0);CACHE.put(name,node);return node;
        }
    }
    private static boolean methodExists(String owner,String name,String desc,Set<String> visited) throws Exception {
        if(owner==null || !visited.add(owner))return false;
        var node=read(owner);
        if(node.methods.stream().anyMatch(m->m.name.equals(name)&&m.desc.equals(desc)))return true;
        if(name.equals("<init>"))return false;
        if(methodExists(node.superName,name,desc,visited))return true;
        for(String parent:node.interfaces)if(methodExists(parent,name,desc,visited))return true;
        return false;
    }
    private static boolean fieldExists(String owner,String name,String desc,Set<String> visited) throws Exception {
        if(owner==null || !visited.add(owner))return false;
        var node=read(owner);
        if(node.fields.stream().anyMatch(f->f.name.equals(name)&&f.desc.equals(desc)))return true;
        if(fieldExists(node.superName,name,desc,visited))return true;
        for(String parent:node.interfaces)if(fieldExists(parent,name,desc,visited))return true;
        return false;
    }
    public static void main(String[] args) throws Exception {
        require(read("dev/protonblade/ProtonBladeItem").superName.equals(SB+"item/ItemSlashBlade"),"Native item base");
        try(var jar=new JarFile(args[0])) {
            var entries=jar.entries();
            while(entries.hasMoreElements()) {
                String name=entries.nextElement().getName();
                if(!name.endsWith(".class"))continue;
                var node=read(name.substring(0,name.length()-6));
                for(var method:node.methods)for(var instruction:method.instructions) {
                    if(instruction instanceof MethodInsnNode call) {
                        require(!call.owner.startsWith("cn/mmf/")&&!call.owner.startsWith("org/scex/"),"Reference-only addon call");
                        if(call.owner.startsWith(SB)||call.owner.startsWith(SI)) {
                            references++;
                            require(methodExists(call.owner,call.name,call.desc,new HashSet<>()),"Unresolved API: "+call.owner+"."+call.name+call.desc);
                        }
                        require(!call.name.equals("doForceHitEntity"),"Do not collapse native wave collision to a forced single-target hit");
                    }
                    if(instruction instanceof FieldInsnNode field && (field.owner.startsWith(SB)||field.owner.startsWith(SI))) {
                        references++;
                        require(fieldExists(field.owner,field.name,field.desc,new HashSet<>()),"Unresolved field: "+field.owner+"."+field.name);
                    }
                }
            }
        }
        System.out.println("PASS: "+references+" native SI/Resharpened member references resolved. No reference-addon calls or forced single-target hits.");
    }
}
