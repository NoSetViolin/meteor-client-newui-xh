import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * NeoForge 的 AT 只会改 class 自身的 access flags，不会改 InnerClasses 表，
 * 因此 javac 仍然认为这些嵌套类是 private（Fabric 的 access widener 会一并改写）。
 *
 * 本工具读取 AT 文件里所有含 '$' 的类条目，把 jar 内所有 InnerClasses 表项里
 * 对应条目的访问级别改成 public，产出可编译的 MC jar。
 *
 * 用法: java -cp asm.jar FixInnerClasses <inJar> <outJar> <atFile>...
 */
public final class FixInnerClasses {
    public static void main(String[] args) throws IOException {
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);

        Set<String> targets = new HashSet<>();
        for (int i = 2; i < args.length; i++) {
            for (String line : Files.readAllLines(Path.of(args[i]))) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                String[] parts = t.split("\\s+");
                if (parts.length < 2) continue;
                String name = parts[1].replace('.', '/');
                if (name.contains("$")) targets.add(name);
            }
        }
        System.out.println("targets: " + targets.size());
        targets.forEach(t -> System.out.println("  " + t));

        int patched = 0;
        try (ZipFile zip = new ZipFile(in.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(out))) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                byte[] data;
                try (InputStream is = zip.getInputStream(e)) {
                    data = is.readAllBytes();
                }
                if (e.getName().endsWith(".class")) {
                    byte[] fixed = patch(data, targets);
                    if (fixed != null) { data = fixed; patched++; }
                }
                zos.putNextEntry(new ZipEntry(e.getName()));
                zos.write(data);
                zos.closeEntry();
            }
        }
        System.out.println("patched classes: " + patched + " -> " + out);
    }

    private static byte[] patch(byte[] data, Set<String> targets) {
        ClassReader cr = new ClassReader(data);
        ClassWriter cw = new ClassWriter(0);
        final boolean[] changed = {false};
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visit(int version, int access, String name, String sig, String sup, String[] ifs) {
                if (targets.contains(name) && (access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) != 0) {
                    access = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                    changed[0] = true;
                }
                super.visit(version, access, name, sig, sup, ifs);
            }

            @Override
            public void visitInnerClass(String name, String outer, String inner, int access) {
                if (targets.contains(name)) {
                    int na = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                    if (na != access) changed[0] = true;
                    access = na;
                }
                super.visitInnerClass(name, outer, inner, access);
            }
        };
        cr.accept(cv, 0);
        return changed[0] ? cw.toByteArray() : null;
    }
}
