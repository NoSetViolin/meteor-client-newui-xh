package meteordevelopment.meteorclient.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public class ReflectInit {
   private static final List<Reflections> reflections = new ArrayList<>();

   private ReflectInit() {
   }

   public static void registerPackages() {
      for (MeteorAddon addon : AddonManager.ADDONS) {
         try {
            add(addon);
         } catch (AbstractMethodError var3) {
            throw new RuntimeException("Addon \"%s\" is too old and cannot be ran.".formatted(addon.name), var3);
         }
      }
   }

   private static void add(MeteorAddon addon) {
      if (addon == meteordevelopment.meteorclient.MeteorClient.ADDON) return;
      String pkg = addon.getPackage();
      if (pkg != null && !pkg.isBlank()) {
         reflections.add(new Reflections(pkg, Scanners.MethodsAnnotated));
      }
   }

   public static void init(Class<? extends Annotation> annotation) {
      runTasks(builtinTasks(annotation), annotation);
      for (Reflections reflection : reflections) {
         Set<Method> initTasks = reflection.getMethodsAnnotatedWith(annotation);
         if (initTasks == null) {
            return;
         }

         runTasks(initTasks, annotation);
      }
   }

   private static Set<Method> builtinTasks(Class<? extends Annotation> annotation) {
      Set<Method> tasks = new HashSet<>();
      try (var input = ReflectInit.class.getResourceAsStream("/meteor-client-init.txt")) {
         if (input == null) throw new IllegalStateException("Missing Meteor initialization index");
         try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String line : reader.lines().toList()) {
               String[] parts = line.split(" ");
               if (!parts[0].equals(annotation.getSimpleName())) continue;
               Class<?> owner = Class.forName(parts[1], false, ReflectInit.class.getClassLoader());
               tasks.add(owner.getDeclaredMethod(parts[2]));
            }
         }
      } catch (ReflectiveOperationException | java.io.IOException e) {
         throw new IllegalStateException("Cannot load Meteor initialization index", e);
      }
      if (tasks.isEmpty()) throw new IllegalStateException("No tasks indexed for " + annotation.getSimpleName());
      return tasks;
   }

   private static void runTasks(Set<Method> tasks, Class<? extends Annotation> annotation) {
      Map<Class<?>, List<Method>> byClass = tasks.stream().collect(Collectors.groupingBy(Method::getDeclaringClass));
      Set<Method> left = new HashSet<>(tasks);
      Method m;
      while ((m = left.stream().findAny().orElse(null)) != null) reflectInit(m, annotation, left, byClass);
   }

   private static <T extends Annotation> void reflectInit(Method task, Class<T> annotation, Set<Method> left, Map<Class<?>, List<Method>> byClass) {
      left.remove(task);

      for (Class<?> clazz : getDependencies(task, annotation)) {
         for (Method m : byClass.getOrDefault(clazz, Collections.emptyList())) {
            if (left.contains(m)) {
               reflectInit(m, annotation, left, byClass);
            }
         }
      }

      try {
         task.invoke(null);
      } catch (InvocationTargetException | IllegalAccessException var10) {
         throw new IllegalStateException(
            "Error running @%s task '%s.%s'".formatted(annotation.getSimpleName(), task.getDeclaringClass().getSimpleName(), task.getName()), var10
         );
      } catch (NullPointerException var11) {
         throw new RuntimeException("Method \"%s\" using Init annotations from non-static context".formatted(task.getName()), var11);
      }
   }

   private static <T extends Annotation> Class<?>[] getDependencies(Method task, Class<T> annotation) {
      T init = task.getAnnotation(annotation);
      Objects.requireNonNull(init);

      return switch (init) {
         case PreInit pre -> pre.dependencies();
         case PostInit post -> post.dependencies();
         default -> new Class[0];
      };
   }
}
