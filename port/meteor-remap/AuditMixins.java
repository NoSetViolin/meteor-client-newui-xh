import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Static selector audit. Runtime application and local capture still require game tests. */
public class AuditMixins {
    static Object val(AnnotationNode a, String key) {
        if (a.values != null) for (int i=0;i<a.values.size();i+=2) if (a.values.get(i).equals(key)) return a.values.get(i+1);
        return null;
    }
    static List<AnnotationNode> annotations(List<AnnotationNode> a, List<AnnotationNode> b) {
        List<AnnotationNode> r=new ArrayList<>(); if(a!=null)r.addAll(a);if(b!=null)r.addAll(b);return r;
    }
    static ClassNode read(byte[] bytes) { ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);return c; }
    public static void main(String[] args) throws Exception {
        Map<String,ClassNode> targets=new HashMap<>();
        try(JarFile jar=new JarFile(args[0])) {
            for(var e: Collections.list(jar.entries())) if(e.getName().endsWith(".class")) {
                ClassNode c=read(jar.getInputStream(e).readAllBytes()); targets.put(c.name,c);
            }
        }
        try(var files=Files.walk(Path.of(args[1]))) {
            for(Path p:files.filter(p->p.toString().endsWith(".class")).toList()) {
                ClassNode mix=read(Files.readAllBytes(p));
                for(var ann:annotations(mix.visibleAnnotations,mix.invisibleAnnotations)) if(ann.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                    List<String> owners=new ArrayList<>();
                    if(val(ann,"value") instanceof List<?> types)for(Object t:types)owners.add(((Type)t).getInternalName());
                    if(val(ann,"targets") instanceof List<?> names)for(Object n:names)owners.add(n.toString().replace('.','/'));
                    for(String owner:owners) {
                        ClassNode c=targets.get(owner); if(c==null){System.out.println(mix.name+"\tCLASS_MISSING\t"+owner);continue;}
                        for(MethodNode m:mix.methods)for(var a:annotations(m.visibleAnnotations,m.invisibleAnnotations)) {
                            if(a.desc.endsWith("/Accessor;")) {
                                String field=(String)val(a,"value");
                                if(field==null||field.isEmpty()) {String stem=m.name.replaceFirst("^(get|set|is)","");field=Character.toLowerCase(stem.charAt(0))+stem.substring(1);}
                                Type type=Type.getReturnType(m.desc);String fd=type.getSort()==Type.VOID?Type.getArgumentTypes(m.desc)[0].getDescriptor():type.getDescriptor();
                                final String f=field.contains(":")?field.substring(0,field.indexOf(':')):field;
                                if(c.fields.stream().noneMatch(n->n.name.equals(f)&&n.desc.equals(fd)))System.out.println(mix.name+"#"+m.name+"\tFIELD_MISSING\t"+field+":"+fd+" candidates="+c.fields.stream().filter(n->n.desc.equals(fd)).map(n->n.name).toList());
                            }
                            if(!(val(a,"method") instanceof List<?> selectors))continue;
                            for(Object s:selectors) {
                                String selector=s.toString();String raw=selector.contains(";")&&selector.startsWith("L")?selector.substring(selector.indexOf(';')+1):selector;
                                int paren=raw.indexOf('(');String name=paren<0?raw:raw.substring(0,paren);String desc=paren<0?null:raw.substring(paren);
                                List<MethodNode> matched=c.methods.stream().filter(n->n.name.equals(name)&&(desc==null||n.desc.equals(desc))).toList();
                                if(matched.isEmpty()){System.out.println(mix.name+"#"+m.name+"\tMETHOD_MISSING\t"+selector);continue;}
                                Object at=val(a,"at");List<?> ats=at instanceof List<?> l?l:at==null?List.of():List.of(at);
                                for(Object obj:ats)if(obj instanceof AnnotationNode point) {
                                    Object target=val(point,"target"); if(target==null)continue;
                                    String t=target.toString(); if(!t.startsWith("L")||!t.contains(";"))continue;
                                    String o=t.substring(1,t.indexOf(';'));String rest=t.substring(t.indexOf(';')+1);boolean found=false;
                                    for(MethodNode n:matched)for(AbstractInsnNode ins:n.instructions) {
                                        if(ins instanceof MethodInsnNode mi && (mi.owner.equals(o)) && (mi.name+mi.desc).equals(rest))found=true;
                                        if(ins instanceof FieldInsnNode fi && fi.owner.equals(o) && (fi.name+":"+fi.desc).equals(rest))found=true;
                                    }
                                    if(!found)System.out.println(mix.name+"#"+m.name+"\tINSTRUCTION_MISSING\t"+selector+" -> "+t);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
