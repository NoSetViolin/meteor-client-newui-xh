import org.objectweb.asm.*;
import java.nio.file.*;
import java.util.*;

/** Generate an explicit index; Reflections does not reliably scan ModLauncher's union URLs. */
public class GenerateInitIndex {
    public static void main(String[] args) throws Exception {
        List<String> entries = new ArrayList<>();
        try (var files = Files.walk(Path.of(args[0]))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                ClassReader reader = new ClassReader(Files.readAllBytes(path));
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            public AnnotationVisitor visitAnnotation(String type, boolean visible) {
                                for (String phase : List.of("PreInit", "PostInit")) {
                                    if (type.equals("Lmeteordevelopment/meteorclient/utils/" + phase + ";")) {
                                        if ((access & Opcodes.ACC_STATIC) == 0 || !desc.equals("()V"))
                                            throw new IllegalStateException("Invalid init method: " + reader.getClassName() + "#" + name);
                                        entries.add(phase + " " + reader.getClassName().replace('/', '.') + " " + name);
                                    }
                                }
                                return null;
                            }
                        };
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        Collections.sort(entries);
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());
        Files.write(output, entries);
        System.out.println("Meteor initialization index: " + entries.size() + " methods");
    }
}
